//
// $Id$

package com.threerings.msoy.avrg.server;

import com.threerings.msoy.avrg.data.AVRMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link AVRProvider}.
 */
public class AVRDispatcher extends InvocationDispatcher<AVRMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public AVRDispatcher (AVRProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public AVRMarshaller createMarshaller ()
    {
        return new AVRMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case AVRMarshaller.ACTIVATE_GAME:
            ((AVRProvider)provider).activateGame(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case AVRMarshaller.DEACTIVATE_GAME:
            ((AVRProvider)provider).deactivateGame(
                source, ((Integer)args[0]).intValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
