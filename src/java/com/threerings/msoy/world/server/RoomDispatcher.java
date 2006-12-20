//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.world.data.RoomMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.whirled.data.SceneUpdate;

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
        case RoomMarshaller.EDIT_ROOM:
            ((RoomProvider)provider).editRoom(
                source,
                (InvocationService.ResultListener)args[0]
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
