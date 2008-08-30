//
// $Id$

package com.threerings.msoy.avrg.server;

import com.threerings.msoy.avrg.data.AVRGameAgentMarshaller;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link AVRGameAgentProvider}.
 */
public class AVRGameAgentDispatcher extends InvocationDispatcher<AVRGameAgentMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public AVRGameAgentDispatcher (AVRGameAgentProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public AVRGameAgentMarshaller createMarshaller ()
    {
        return new AVRGameAgentMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case AVRGameAgentMarshaller.LEAVE_GAME:
            ((AVRGameAgentProvider)provider).leaveGame(
                source, ((Integer)args[0]).intValue()
            );
            return;

        case AVRGameAgentMarshaller.ROOM_SUBSCRIPTION_COMPLETE:
            ((AVRGameAgentProvider)provider).roomSubscriptionComplete(
                source, ((Integer)args[0]).intValue()
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
