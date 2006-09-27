//
// $Id$

package com.threerings.msoy.world.server;

import com.samskivert.util.RandomUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.server.SpotSceneManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MemberName;
import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.RoomMarshaller;

/**
 * Manages a "Room".
 */
public class RoomManager extends SpotSceneManager
    implements RoomProvider
{
    @Override
    protected void didStartup ()
    {
        super.didStartup();

        _roomObj = (RoomObject) _plobj;

        _roomObj.setRoomService(
            (RoomMarshaller) MsoyServer.invmgr.registerDispatcher(
            new RoomDispatcher(this), false));
    }

    @Override
    protected void didShutdown ()
    {
        MsoyServer.invmgr.clearDispatcher(_roomObj.roomService);

        super.didShutdown();
    }


    @Override // documentation inherited
    protected SceneLocation computeEnteringLocation (
        BodyObject body, Portal entry)
    {
        MemberObject memberObj = (MemberObject) body;

        // automatically add the room to their recent list
        memberObj.addToRecentScenes(_scene.getId(), _scene.getName());

        if (entry != null) {
            return super.computeEnteringLocation(body, entry);
        }

        // fallback if there is no portal
        return new SceneLocation(
            new MsoyLocation(.5, 0, .5, (short) 0), body.getOid());
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new RoomObject();
    }

    // documentation inherited from RoomProvider
    public void updateRoom (
        ClientObject caller, SceneUpdate[] updates,
        InvocationListener listener)
        throws InvocationException
    {
        if (!((MsoyScene) _scene).canEdit((MemberObject) caller)) {
            throw new InvocationException(ACCESS_DENIED);
        }

        for (SceneUpdate update : updates) {
            System.err.println("Got update: " + update);
            recordUpdate(update);
        }
    }

    /** The room object. */
    protected RoomObject _roomObj;
}
