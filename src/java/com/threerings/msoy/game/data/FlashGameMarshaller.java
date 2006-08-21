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
    /** The method id used to dispatch {@link #addToCollection} requests. */
    public static final int ADD_TO_COLLECTION = 1;

    // documentation inherited from interface
    public void addToCollection (Client arg1, String arg2, byte[][] arg3, boolean arg4, InvocationService.InvocationListener arg5)
    {
        ListenerMarshaller listener5 = new ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, ADD_TO_COLLECTION, new Object[] {
            arg2, arg3, Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #endGame} requests. */
    public static final int END_GAME = 2;

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
    public static final int END_TURN = 3;

    // documentation inherited from interface
    public void endTurn (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, END_TURN, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getFromCollection} requests. */
    public static final int GET_FROM_COLLECTION = 4;

    // documentation inherited from interface
    public void getFromCollection (Client arg1, String arg2, boolean arg3, int arg4, String arg5, int arg6, InvocationService.ConfirmListener arg7)
    {
        InvocationMarshaller.ConfirmMarshaller listener7 = new InvocationMarshaller.ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, GET_FROM_COLLECTION, new Object[] {
            arg2, Boolean.valueOf(arg3), Integer.valueOf(arg4), arg5, Integer.valueOf(arg6), listener7
        });
    }

    /** The method id used to dispatch {@link #mergeCollection} requests. */
    public static final int MERGE_COLLECTION = 5;

    // documentation inherited from interface
    public void mergeCollection (Client arg1, String arg2, String arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, MERGE_COLLECTION, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #sendMessage} requests. */
    public static final int SEND_MESSAGE = 6;

    // documentation inherited from interface
    public void sendMessage (Client arg1, String arg2, Object arg3, int arg4, InvocationService.InvocationListener arg5)
    {
        ListenerMarshaller listener5 = new ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SEND_MESSAGE, new Object[] {
            arg2, arg3, Integer.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #setProperty} requests. */
    public static final int SET_PROPERTY = 7;

    // documentation inherited from interface
    public void setProperty (Client arg1, String arg2, Object arg3, int arg4, InvocationService.InvocationListener arg5)
    {
        ListenerMarshaller listener5 = new ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_PROPERTY, new Object[] {
            arg2, arg3, Integer.valueOf(arg4), listener5
        });
    }

}
