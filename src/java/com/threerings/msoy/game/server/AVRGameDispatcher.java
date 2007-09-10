//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.AVRGameService;
import com.threerings.msoy.game.data.AVRGameMarshaller;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link AVRGameProvider}.
 */
public class AVRGameDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public AVRGameDispatcher (AVRGameProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new AVRGameMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case AVRGameMarshaller.JOIN_AVRGAME:
            ((AVRGameProvider)provider).joinAVRGame(
                source,
                ((Integer)args[0]).intValue(), (InvocationService.InvocationListener)args[1]
            );
            return;

        case AVRGameMarshaller.LEAVE_AVRGAME:
            ((AVRGameProvider)provider).leaveAVRGame(
                source,
                (InvocationService.InvocationListener)args[0]
            );
            return;

        case AVRGameMarshaller.UPDATE_MEMORY:
            ((AVRGameProvider)provider).updateMemory(
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
