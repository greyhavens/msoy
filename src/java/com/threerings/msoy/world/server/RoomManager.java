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
        int memberId = RandomUtil.getInt(10000);
        ((MemberObject) body).addToFriends(
            new FriendEntry(new MemberName(String.valueOf(memberId), memberId),
                (RandomUtil.getInt(2) == 0),
                (byte) RandomUtil.getInt(3)));

        if (entry != null) {
            return super.computeEnteringLocation(body, entry);
        }

        // fallback if there is no portal
        return new SceneLocation(
            new MsoyLocation(0, 0, 0, (short) 0), body.getOid());
    }

    @Override
    protected Class<? extends PlaceObject> getPlaceObjectClass ()
    {
        return RoomObject.class;
    }

    // documentation inherited from RoomProvider
    public void updateRoom (
            ClientObject caller, SceneUpdate[] updates,
            InvocationListener listener)
        throws InvocationException
    {
        // TODO: validate that the client has permissions to edit this room

        for (SceneUpdate update : updates) {
            System.err.println("Got update: " + update);
            recordUpdate(update);
        }
    }

    /** The room object. */
    protected RoomObject _roomObj;
}
