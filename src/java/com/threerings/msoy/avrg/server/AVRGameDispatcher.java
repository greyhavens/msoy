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
        case AVRGameMarshaller.CANCEL_QUEST:
            ((AVRGameProvider)provider).cancelQuest(
                source, (String)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case AVRGameMarshaller.COMPLETE_QUEST:
            ((AVRGameProvider)provider).completeQuest(
                source, (String)args[0], ((Float)args[1]).floatValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case AVRGameMarshaller.SET_TICKER:
            ((AVRGameProvider)provider).setTicker(
                source, (String)args[0], ((Integer)args[1]).intValue(), (InvocationService.InvocationListener)args[2]
            );
            return;

        case AVRGameMarshaller.START_QUEST:
            ((AVRGameProvider)provider).startQuest(
                source, (String)args[0], (String)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case AVRGameMarshaller.UPDATE_QUEST:
            ((AVRGameProvider)provider).updateQuest(
                source, (String)args[0], ((Integer)args[1]).intValue(), (String)args[2], (InvocationService.ConfirmListener)args[3]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
