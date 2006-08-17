//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;

import com.threerings.util.Integer;

import com.threerings.io.TypedArray;

import com.threerings.msoy.game.client.FlashGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
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
    public static const END_GAME :int = 1;

    // documentation inherited from interface
    public function endGame (arg1 :Client, arg2 :TypedArray, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, END_GAME, [
            arg2, listener3
        ]);
    }

    /** The method id used to dispatch {@link #endTurn} requests. */
    public static const END_TURN :int = 2;

    // documentation inherited from interface
    public function endTurn (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, END_TURN, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch {@link #sendMessage} requests. */
    public static const SEND_MESSAGE :int = 3;

    // documentation inherited from interface
    public function sendMessage (arg1 :Client, arg2 :int, arg3 :String, arg4 :ByteArray, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SEND_MESSAGE, [
            Integer.valueOf(arg2), arg3, arg4, listener5
        ]);
    }

}
}
