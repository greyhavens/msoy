//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;

import com.threerings.util.langBoolean;
import com.threerings.util.Integer;

import com.threerings.io.TypedArray;

import com.threerings.msoy.game.client.FlashGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
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
    /** The method id used to dispatch {@link #addToCollection} requests. */
    public static const ADD_TO_COLLECTION :int = 1;
    
    // documentation inherited from interface
    public function addToCollection (arg1 :Client, arg2 :String, arg3 :TypedArray /* of ByteArray */, arg4 :Boolean, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, ADD_TO_COLLECTION, [
            arg2, arg3, langBoolean.valueOf(arg4), listener5
        ]);
    }

    /** The method id used to dispatch {@link #endGame} requests. */
    public static const END_GAME :int = 2;

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
    public static const END_TURN :int = 3;

    // documentation inherited from interface
    public function endTurn (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, END_TURN, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch {@link #getFromCollection} requests. */
    public static const GET_FROM_COLLECTION :int = 4;

    // documentation inherited from interface
    public function getFromCollection (arg1 :Client, arg2 :String, arg3 :Boolean, arg4 :int, arg5 :String, arg6 :int, arg7 :InvocationService_ConfirmListener) :void
    {
        var listener7 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, GET_FROM_COLLECTION, [
            arg2, langBoolean.valueOf(arg3), Integer.valueOf(arg4), arg5, Integer.valueOf(arg6), listener7
        ]);
    }

    /** The method id used to dispatch {@link #mergeCollection} requests. */
    public static const MERGE_COLLECTION :int = 5;

    // documentation inherited from interface
    public function mergeCollection (arg1 :Client, arg2 :String, arg3 :String, arg4 :InvocationService_InvocationListener) :void
    {
        var listener4 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, MERGE_COLLECTION, [
            arg2, arg3, listener4
        ]);
    }

    /** The method id used to dispatch {@link #sendMessage} requests. */
    public static const SEND_MESSAGE :int = 6;

    // documentation inherited from interface
    public function sendMessage (arg1 :Client, arg2 :String, arg3 :Object, arg4 :int, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SEND_MESSAGE, [
            arg2, arg3, Integer.valueOf(arg4), listener5
        ]);
    }

    /** The method id used to dispatch {@link #setProperty} requests. */
    public static const SET_PROPERTY :int = 7;

    // documentation inherited from interface
    public function setProperty (arg1 :Client, arg2 :String, arg3 :Object, arg4 :int, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_PROPERTY, [
            arg2, arg3, Integer.valueOf(arg4), listener5
        ]);
    }

}
}
