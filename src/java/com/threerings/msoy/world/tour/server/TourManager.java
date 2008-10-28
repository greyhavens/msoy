//
// $Id$

package com.threerings.msoy.world.tour.server;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.util.StreamableArrayIntSet;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.StatLogic;

import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages folks who are touring around.
 */
@Singleton @EventThread
public class TourManager
    implements TourProvider, ShutdownManager.Shutdowner
{
    @Inject public TourManager (InvocationManager invmgr, ShutdownManager shutmgr)
    {
        invmgr.registerDispatcher(new TourDispatcher(this), MsoyCodes.WORLD_GROUP);
        shutmgr.registerShutdowner(this);
    }

    /**
     * Called when the server is ready to roll.
     */
    public void init ()
    {
        loadNewRoomSet();
        _roomSetRefresher = new Interval(_omgr) {
            public void expired () {
                loadNewRoomSet();
            }
        };
        _roomSetRefresher.schedule(ROOM_SET_REFRESH, true);
    }

    // from interface ShutdownManager.Shutdowner
    public void shutdown ()
    {
        _roomSetRefresher.cancel();
        _roomSetRefresher = null;
    }

    // from TourProvider
    public void nextRoom (
        ClientObject caller, boolean finishedLoadingCurrentRoom,
        InvocationService.ResultListener listener)
        throws InvocationException
    {
        MemberObject memObj = (MemberObject) caller;

        // put them "on tour" if they're not already
        if (!memObj.onTour) {
            memObj.setOnTour(true);
            memObj.getLocal(MemberLocal.class).touredRooms = new StreamableArrayIntSet();
        }

        int nextRoom = pickNextRoom(memObj);
        memObj.getLocal(MemberLocal.class).touredRooms.add(nextRoom);
        listener.requestProcessed(nextRoom);
        // maybe increment the user's TOURED stat
        if (finishedLoadingCurrentRoom && !memObj.isGuest()) {
            _statLogic.incrementStat(memObj.getMemberId(), StatType.ROOMS_TOURED, 1);
        }
    }

    // from TourProvider
    public void endTour (ClientObject caller)
    {
        MemberObject memObj = (MemberObject) caller;

        // stop their tour if they're on one
        if (memObj.onTour) {
            memObj.setOnTour(false);
            memObj.getLocal(MemberLocal.class).touredRooms = null; // forget any toured rooms
        }
    }

    /**
     * Pick the next room for this user.
     */
    protected int pickNextRoom (MemberObject memObj)
        throws InvocationException
    {
        if (_rooms == null) {
            throw new InvocationException("e.toured_out");
        }

        PopularPlacesSnapshot snapshot = _memberMan.getPPSnapshot();
        for (int ii = 0, nn = _rooms.length; ii < nn; ii++) {
            int sceneId = _rooms[ii];
            PopularPlacesSnapshot.Place place = snapshot.getScene(sceneId);
            if ((place == null || place.population < MAX_ROOM_POPULATION) &&
                    isValidNextRoom(memObj, sceneId)) {
                if (ii > 0) {
                    // since we're actually placing someone in this room, move it forward one
                    // spot in the list.
                    // TODO: hmm, maybe we should move it to the front of the list?
                    // Or move overpopulated rooms to the end of the list?
                    _rooms[ii] = _rooms[ii - 1];
                    _rooms[ii - 1] = sceneId;
                }
                return sceneId;
            }
        }

        // try 10 times to pick a random room that's already resolved
        for (int ii = 0; ii < 10; ii++) {
            PopularPlacesSnapshot.Place place = snapshot.getRandomResolvedScene();
            if (place == null) {
                break;
            }
            if (place.population < MAX_ROOM_POPULATION && isValidNextRoom(memObj, place.placeId)) {
                // if this place is a valid for this user then we know that it wasn't already
                // in our list of rooms, because all those were invalid. Add it to the end.
                int[] newRooms = new int[_rooms.length + 1];
                System.arraycopy(_rooms, 0, newRooms, 0, _rooms.length);
                newRooms[_rooms.length] = place.placeId;
                _rooms = newRooms;
                return place.placeId;
            }
        }

        log.warning("TourManager could not find a room for a touring user!");
        throw new InvocationException("e.toured_out");
    }

    /**
     * Determines if the specified room is valid for the user.
     */
    protected boolean isValidNextRoom (MemberObject memObj, int sceneId)
    {
        return (memObj.homeSceneId != sceneId) &&
            (memObj.getSceneId() != sceneId) &&
            !memObj.getLocal(MemberLocal.class).touredRooms.contains(sceneId);
    }

    protected void loadNewRoomSet ()
    {
        _invoker.postUnit(new RepositoryUnit("loadTourRooms") {
            public void invokePersist ()
                throws Exception
            {
                List<SceneRecord> scenes = _sceneRepo.loadScenes(0, ROOMS_PER_SET);
                _newRooms = new int[scenes.size()];
                for (int ii = 0, nn = scenes.size(); ii < nn; ii++) {
                    _newRooms[ii] = scenes.get(ii).sceneId;
                }
            }

            public void handleSuccess ()
            {
                // now use the new rooms
                _rooms = _newRooms;
            }

            protected int[] _newRooms;
        });
    }

    /** The number of new & hot rooms loaded for the tour. */
    protected static final int ROOMS_PER_SET = 50;

    /** The number of occupants a room must have before the tour skips over it. */
    protected static final int MAX_ROOM_POPULATION = 18;

    /** The milliseconds between refreshes of our room set. */
    protected static final long ROOM_SET_REFRESH = 2 * 60 * 1000L; // 2 minutes

    /** The current list of tour rooms, in order of preference. */
    protected int[] _rooms;

    /** Updates our set of rooms. */
    protected Interval _roomSetRefresher;

    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected StatLogic _statLogic;
    @Inject protected MemberManager _memberMan;
    @Inject protected MsoySceneRepository _sceneRepo;
}
