//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.WorldGameMarshaller;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link WorldGameProvider}.
 */
public class WorldGameDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public WorldGameDispatcher (WorldGameProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new WorldGameMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case WorldGameMarshaller.JOIN_WORLD_GAME:
            ((WorldGameProvider)provider).joinWorldGame(
                source,
                ((Integer)args[0]).intValue(), (InvocationService.InvocationListener)args[1]
            );
            return;

        case WorldGameMarshaller.LEAVE_WORLD_GAME:
            ((WorldGameProvider)provider).leaveWorldGame(
                source,
                (InvocationService.InvocationListener)args[0]
            );
            return;

        case WorldGameMarshaller.UPDATE_MEMORY:
            ((WorldGameProvider)provider).updateMemory(
                source,
                (MemoryEntry)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
