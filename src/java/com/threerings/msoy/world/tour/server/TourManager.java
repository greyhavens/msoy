//
// $Id$

package com.threerings.msoy.world.tour.server;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
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
import com.threerings.presents.peer.data.NodeObject;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.server.StatLogic;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

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

    // Use _sceneRepo.loadScenes(0, 50); // to load the latest new and hot
    // store those in some sort of sorted interface
    // 
    // maintain a record with a population count for each room (listening on node objects)
    // when a user requests a room, skip past overpopulated rooms
    // when you find a room for that user, move it up the list one spot

    // from TourProvider
    public void nextRoom (ClientObject caller, InvocationService.ResultListener listener)
        throws InvocationException
    {
        MemberObject memObj = (MemberObject) caller;

        // put them "on tour" if they're not already
        if (!memObj.onTour) {
            memObj.setOnTour(true);
            memObj.touredRooms = new StreamableArrayIntSet();
        }

        int nextRoom = pickNextRoom(memObj);
        //dumpRooms();
        memObj.touredRooms.add(nextRoom);
        listener.requestProcessed(nextRoom);
        // go ahead and increment the user's TOURED stat
        if (!memObj.isGuest()) {
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
            memObj.touredRooms = null; // forget any toured rooms
        }
    }

    /**
     * Pick the next room for this user.
     */
    protected int pickNextRoom (MemberObject memObj)
    {
        if (_rooms == null) {
            // whoa! Send'em to BNW
            return 1; // TODO?
        }

        for (int ii = 0, nn = _rooms.size(); ii < nn; ii++) {
            RoomRecord room = _rooms.get(ii);
            if ((room.occupants < MAX_ROOM_POPULATION) && isValidNextRoom(memObj, room.sceneId)) {
                if (ii > 0) {
                    // since we're actually placing someone in this room, move it forward one
                    // spot in the list.
                    // TODO: hmm, maybe we should move it to the front of the list?
                    // Or move overpopulated rooms to the end of the list?
                    _rooms.set(ii, _rooms.set(ii - 1, room));
                }
                return room.sceneId;
            }
        }

        // TODO: load up a random-ass room and add it to the list? We'd have to refigure population
        // so that is harsh.
        log.warning("Oh-serious-no! Ran out of rooms for touring!");
        return 1;
    }

    /**
     * Determines if the specified room is valid for the user.
     */
    protected boolean isValidNextRoom (MemberObject memObj, int sceneId)
    {
        return (memObj.homeSceneId != sceneId) &&
            !memObj.touredRooms.contains(sceneId) &&
            (memObj.getSceneId() != sceneId);
    }

    protected void loadNewRoomSet ()
    {
        _invoker.postUnit(new RepositoryUnit("loadTourRooms") {
            public void invokePersist ()
                throws Exception
            {
                _scenes = _sceneRepo.loadScenes(0, ROOMS_PER_SET);
            }

            public void handleSuccess ()
            {
                // create our list of rooms
                List<RoomRecord> rooms = Lists.newArrayList();
                final IntMap<RoomRecord> roomsById = IntMaps.newHashIntMap();
                for (SceneRecord scene : _scenes) {
                    RoomRecord room = new RoomRecord(scene.sceneId);
                    rooms.add(room);
                    roomsById.put(scene.sceneId, room);
                }
                // figure out the population in each
                // TODO: this could be a bad idea, this is a lot of iteration...
                _peerMan.applyToNodes(new Function<NodeObject,Void>() {
                    public Void apply (NodeObject nobj) {
                        for (MemberLocation mloc : ((MsoyNodeObject) nobj).memberLocs) {
                            if (mloc.sceneId != 0) {
                                RoomRecord room = roomsById.get(mloc.sceneId);
                                if (room != null) {
                                    room.occupants++;
                                }
                            }
                        }
                        return null;
                    }
                });
                // now use these new lists
                _rooms = rooms;
                _roomsById = roomsById;
                //dumpRooms();
            }

            protected List<SceneRecord> _scenes;
        });
    }

    /** Tracks room populations. */
    protected static class RoomRecord
    {
        public int sceneId;

        public int occupants;

        public RoomRecord (int sceneId)
        {
            this.sceneId = sceneId;
        }
    }

//    // some debugging:
//    protected void dumpRooms ()
//    {
//        System.err.println("==== Rooms: ");
//        for (RoomRecord rec : _rooms) {
//            System.err.print(" " + rec.sceneId);
//        }
//        System.err.println();
//    }

    /** The number of new & hot rooms loaded for the tour. */
    protected static final int ROOMS_PER_SET = 50;

    /** The number of occupants a room must have before the tour skips over it. */
    protected static final int MAX_ROOM_POPULATION = 15;

    /** The milliseconds between refreshes of our room set. */
    protected static final long ROOM_SET_REFRESH = 2 * 60 * 1000L; // 2 minutes

    /** The current list of tour rooms, in order of preference. */
    protected List<RoomRecord> _rooms;

    /** A mapping of sceneId -> RoomRecord, so we can update population counts. */
    protected IntMap<RoomRecord> _roomsById;

    /** Updates our set of rooms. */
    protected Interval _roomSetRefresher;

//    protected MsoyPeerManager.MemberObserver _memberObserver =
//        new MsoyPeerManager.MemberObserver() {
//            public void memberLoggedOn (String node, MemberName member)
//        };

    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected StatLogic _statLogic;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MsoySceneRepository _sceneRepo;
}
