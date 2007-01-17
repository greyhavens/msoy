//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.world.client.RoomService;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.RoomMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

/**
 * Dispatches requests to the {@link RoomProvider}.
 */
public class RoomDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public RoomDispatcher (RoomProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new RoomMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case RoomMarshaller.CHANGE_LOCATION:
            ((RoomProvider)provider).changeLocation(
                source,
                (ItemIdent)args[0], (Location)args[1]
            );
            return;

        case RoomMarshaller.EDIT_ROOM:
            ((RoomProvider)provider).editRoom(
                source,
                (InvocationService.ResultListener)args[0]
            );
            return;

        case RoomMarshaller.REQUEST_CONTROLLER:
            ((RoomProvider)provider).requestController(
                source,
                (ItemIdent)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case RoomMarshaller.TRIGGER_EVENT:
            ((RoomProvider)provider).triggerEvent(
                source,
                (ItemIdent)args[0], (String)args[1], (byte[])args[2]
            );
            return;

        case RoomMarshaller.UPDATE_MEMORY:
            ((RoomProvider)provider).updateMemory(
                source,
                (MemoryEntry)args[0]
            );
            return;

        case RoomMarshaller.UPDATE_ROOM:
            ((RoomProvider)provider).updateRoom(
                source,
                (SceneUpdate[])args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
