//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.AVRGameService;
import com.threerings.msoy.game.data.AVRGameMarshaller;
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
        case AVRGameMarshaller.COMPLETE_QUEST:
            ((AVRGameProvider)provider).completeQuest(
                source,
                (String)args[0], ((Integer)args[1]).intValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case AVRGameMarshaller.DELETE_PLAYER_PROPERTY:
            ((AVRGameProvider)provider).deletePlayerProperty(
                source,
                (String)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case AVRGameMarshaller.DELETE_PROPERTY:
            ((AVRGameProvider)provider).deleteProperty(
                source,
                (String)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case AVRGameMarshaller.SET_PLAYER_PROPERTY:
            ((AVRGameProvider)provider).setPlayerProperty(
                source,
                (String)args[0], (byte[])args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case AVRGameMarshaller.SET_PROPERTY:
            ((AVRGameProvider)provider).setProperty(
                source,
                (String)args[0], (byte[])args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case AVRGameMarshaller.START_QUEST:
            ((AVRGameProvider)provider).startQuest(
                source,
                (String)args[0], (String)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case AVRGameMarshaller.UPDATE_QUEST:
            ((AVRGameProvider)provider).updateQuest(
                source,
                (String)args[0], ((Integer)args[1]).intValue(), (String)args[2], (InvocationService.ConfirmListener)args[3]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
