//
// $Id$

package com.threerings.msoy.avrg.data {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.
import com.threerings.io.TypedArray;

import com.threerings.msoy.avrg.client.AVRGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;

/**
 * Provides the implementation of the {@link AVRGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class AVRGameMarshaller extends InvocationMarshaller
    implements AVRGameService
{
    /** The method id used to dispatch {@link #cancelQuest} requests. */
    public static const CANCEL_QUEST :int = 1;

    // from interface AVRGameService
    public function cancelQuest (arg1 :Client, arg2 :String, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, CANCEL_QUEST, [
            arg2, listener3
        ]);
    }

    /** The method id used to dispatch {@link #completeQuest} requests. */
    public static const COMPLETE_QUEST :int = 2;

    // from interface AVRGameService
    public function completeQuest (arg1 :Client, arg2 :String, arg3 :int, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, COMPLETE_QUEST, [
            arg2, Integer.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch {@link #deletePlayerProperty} requests. */
    public static const DELETE_PLAYER_PROPERTY :int = 3;

    // from interface AVRGameService
    public function deletePlayerProperty (arg1 :Client, arg2 :String, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DELETE_PLAYER_PROPERTY, [
            arg2, listener3
        ]);
    }

    /** The method id used to dispatch {@link #deleteProperty} requests. */
    public static const DELETE_PROPERTY :int = 4;

    // from interface AVRGameService
    public function deleteProperty (arg1 :Client, arg2 :String, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DELETE_PROPERTY, [
            arg2, listener3
        ]);
    }

    /** The method id used to dispatch {@link #sendMessage} requests. */
    public static const SEND_MESSAGE :int = 5;

    // from interface AVRGameService
    public function sendMessage (arg1 :Client, arg2 :String, arg3 :Object, arg4 :int, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SEND_MESSAGE, [
            arg2, arg3, Integer.valueOf(arg4), listener5
        ]);
    }

    /** The method id used to dispatch {@link #setPlayerProperty} requests. */
    public static const SET_PLAYER_PROPERTY :int = 6;

    // from interface AVRGameService
    public function setPlayerProperty (arg1 :Client, arg2 :String, arg3 :ByteArray, arg4 :Boolean, arg5 :InvocationService_ConfirmListener) :void
    {
        var listener5 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_PLAYER_PROPERTY, [
            arg2, arg3, langBoolean.valueOf(arg4), listener5
        ]);
    }

    /** The method id used to dispatch {@link #setProperty} requests. */
    public static const SET_PROPERTY :int = 7;

    // from interface AVRGameService
    public function setProperty (arg1 :Client, arg2 :String, arg3 :ByteArray, arg4 :Boolean, arg5 :InvocationService_ConfirmListener) :void
    {
        var listener5 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_PROPERTY, [
            arg2, arg3, langBoolean.valueOf(arg4), listener5
        ]);
    }

    /** The method id used to dispatch {@link #startQuest} requests. */
    public static const START_QUEST :int = 8;

    // from interface AVRGameService
    public function startQuest (arg1 :Client, arg2 :String, arg3 :String, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, START_QUEST, [
            arg2, arg3, listener4
        ]);
    }

    /** The method id used to dispatch {@link #updateQuest} requests. */
    public static const UPDATE_QUEST :int = 9;

    // from interface AVRGameService
    public function updateQuest (arg1 :Client, arg2 :String, arg3 :int, arg4 :String, arg5 :InvocationService_ConfirmListener) :void
    {
        var listener5 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, UPDATE_QUEST, [
            arg2, Integer.valueOf(arg3), arg4, listener5
        ]);
    }
}
}
