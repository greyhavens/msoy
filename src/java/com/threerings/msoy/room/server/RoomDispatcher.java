//
// $Id$

package com.threerings.msoy.room.server;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.data.RoomMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

/**
 * Dispatches requests to the {@link RoomProvider}.
 */
public class RoomDispatcher extends InvocationDispatcher<RoomMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public RoomDispatcher (RoomProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public RoomMarshaller createMarshaller ()
    {
        return new RoomMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case RoomMarshaller.CHANGE_LOCATION:
            ((RoomProvider)provider).changeLocation(
                source, (ItemIdent)args[0], (Location)args[1]
            );
            return;

        case RoomMarshaller.DESPAWN_MOB:
            ((RoomProvider)provider).despawnMob(
                source, ((Integer)args[0]).intValue(), (String)args[1], (InvocationService.InvocationListener)args[2]
            );
            return;

        case RoomMarshaller.EDIT_ROOM:
            ((RoomProvider)provider).editRoom(
                source, (InvocationService.ResultListener)args[0]
            );
            return;

        case RoomMarshaller.MOVE_MOB:
            ((RoomProvider)provider).moveMob(
                source, ((Integer)args[0]).intValue(), (String)args[1], (Location)args[2], (InvocationService.InvocationListener)args[3]
            );
            return;

        case RoomMarshaller.PUBLISH_ROOM:
            ((RoomProvider)provider).publishRoom(
                source, (InvocationService.InvocationListener)args[0]
            );
            return;

        case RoomMarshaller.PURCHASE_ROOM:
            ((RoomProvider)provider).purchaseRoom(
                source, (InvocationService.ResultListener)args[0]
            );
            return;

        case RoomMarshaller.RATE_ROOM:
            ((RoomProvider)provider).rateRoom(
                source, ((Byte)args[0]).byteValue(), (InvocationService.InvocationListener)args[1]
            );
            return;

        case RoomMarshaller.REQUEST_CONTROL:
            ((RoomProvider)provider).requestControl(
                source, (ItemIdent)args[0]
            );
            return;

        case RoomMarshaller.SEND_POSTCARD:
            ((RoomProvider)provider).sendPostcard(
                source, (String[])args[0], (String)args[1], (String)args[2], (String)args[3], (InvocationService.ConfirmListener)args[4]
            );
            return;

        case RoomMarshaller.SEND_SPRITE_MESSAGE:
            ((RoomProvider)provider).sendSpriteMessage(
                source, (ItemIdent)args[0], (String)args[1], (byte[])args[2], ((Boolean)args[3]).booleanValue()
            );
            return;

        case RoomMarshaller.SEND_SPRITE_SIGNAL:
            ((RoomProvider)provider).sendSpriteSignal(
                source, (String)args[0], (byte[])args[1]
            );
            return;

        case RoomMarshaller.SET_ACTOR_STATE:
            ((RoomProvider)provider).setActorState(
                source, (ItemIdent)args[0], ((Integer)args[1]).intValue(), (String)args[2]
            );
            return;

        case RoomMarshaller.SPAWN_MOB:
            ((RoomProvider)provider).spawnMob(
                source, ((Integer)args[0]).intValue(), (String)args[1], (String)args[2], (Location)args[3], (InvocationService.InvocationListener)args[4]
            );
            return;

        case RoomMarshaller.UPDATE_MEMORY:
            ((RoomProvider)provider).updateMemory(
                source, (ItemIdent)args[0], (String)args[1], (byte[])args[2], (InvocationService.ResultListener)args[3]
            );
            return;

        case RoomMarshaller.UPDATE_ROOM:
            ((RoomProvider)provider).updateRoom(
                source, (SceneUpdate)args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
