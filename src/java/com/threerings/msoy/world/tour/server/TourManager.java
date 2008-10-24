//
// $Id$

package com.threerings.msoy.world.tour.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.util.StreamableArrayIntSet;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.server.StatLogic;

/**
 * Manages folks who are touring around.
 */
@Singleton @EventThread
public class TourManager
    implements TourProvider
{
    @Inject public TourManager (InvocationManager invmgr)
    {
        invmgr.registerDispatcher(new TourDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

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
        // TODO: Real implementation

        for (int sceneId = 1; sceneId < 1000; sceneId++) {
            if (isValidNextRoom(memObj, sceneId)) {
                return sceneId;
            }
        }
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

    @Inject protected StatLogic _statLogic;
}
