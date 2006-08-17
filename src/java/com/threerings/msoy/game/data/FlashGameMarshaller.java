//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.game.client.FlashGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link FlashGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class FlashGameMarshaller extends InvocationMarshaller
    implements FlashGameService
{
    /** The method id used to dispatch {@link #endGame} requests. */
    public static final int END_GAME = 1;

    // documentation inherited from interface
    public void endGame (Client arg1, int[] arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, END_GAME, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #endTurn} requests. */
    public static final int END_TURN = 2;

    // documentation inherited from interface
    public void endTurn (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, END_TURN, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #sendMessage} requests. */
    public static final int SEND_MESSAGE = 3;

    // documentation inherited from interface
    public void sendMessage (Client arg1, int arg2, String arg3, byte[] arg4, InvocationService.InvocationListener arg5)
    {
        ListenerMarshaller listener5 = new ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SEND_MESSAGE, new Object[] {
            Integer.valueOf(arg2), arg3, arg4, listener5
        });
    }

}
