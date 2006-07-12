//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.world.client.RoomService;
import com.threerings.msoy.world.data.RoomMarshaller;
import com.threerings.presents.client.Client;
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

    // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new RoomMarshaller();
    }

    // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
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
