//
// $Id$

package com.threerings.msoy.avrg.server;

import com.threerings.msoy.avrg.data.AVRGameMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link AVRGameProvider}.
 */
public class AVRGameDispatcher extends InvocationDispatcher<AVRGameMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public AVRGameDispatcher (AVRGameProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public AVRGameMarshaller createMarshaller ()
    {
        return new AVRGameMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case AVRGameMarshaller.COMPLETE_TASK:
            ((AVRGameProvider)provider).completeTask(
                source, ((Integer)args[0]).intValue(), (String)args[1], ((Float)args[2]).floatValue(), (InvocationService.ConfirmListener)args[3]
            );
            return;

        case AVRGameMarshaller.LOAD_OFFLINE_PLAYER:
            ((AVRGameProvider)provider).loadOfflinePlayer(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case AVRGameMarshaller.SET_IDLE:
            ((AVRGameProvider)provider).setIdle(
                source, ((Boolean)args[0]).booleanValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        case AVRGameMarshaller.SET_OFFLINE_PLAYER_PROPERTY:
            ((AVRGameProvider)provider).setOfflinePlayerProperty(
                source, ((Integer)args[0]).intValue(), (String)args[1], args[2], (Integer)args[3], ((Boolean)args[4]).booleanValue(), (InvocationService.ConfirmListener)args[5]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
