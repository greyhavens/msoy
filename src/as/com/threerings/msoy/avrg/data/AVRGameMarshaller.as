//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.msoy.avrg.client.AVRGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.util.Float;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>AVRGameService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class AVRGameMarshaller extends InvocationMarshaller
    implements AVRGameService
{
    /** The method id used to dispatch <code>cancelQuest</code> requests. */
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

    /** The method id used to dispatch <code>completeQuest</code> requests. */
    public static const COMPLETE_QUEST :int = 2;

    // from interface AVRGameService
    public function completeQuest (arg1 :Client, arg2 :String, arg3 :Number, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, COMPLETE_QUEST, [
            arg2, Float.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>roomSubscriptionComplete</code> requests. */
    public static const ROOM_SUBSCRIPTION_COMPLETE :int = 3;

    // from interface AVRGameService
    public function roomSubscriptionComplete (arg1 :Client, arg2 :int) :void
    {
        sendRequest(arg1, ROOM_SUBSCRIPTION_COMPLETE, [
            Integer.valueOf(arg2)
        ]);
    }

    /** The method id used to dispatch <code>sendMessage</code> requests. */
    public static const SEND_MESSAGE :int = 4;

    // from interface AVRGameService
    public function sendMessage (arg1 :Client, arg2 :String, arg3 :Object, arg4 :int, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SEND_MESSAGE, [
            arg2, arg3, Integer.valueOf(arg4), listener5
        ]);
    }

    /** The method id used to dispatch <code>setTicker</code> requests. */
    public static const SET_TICKER :int = 5;

    // from interface AVRGameService
    public function setTicker (arg1 :Client, arg2 :String, arg3 :int, arg4 :InvocationService_InvocationListener) :void
    {
        var listener4 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_TICKER, [
            arg2, Integer.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>startQuest</code> requests. */
    public static const START_QUEST :int = 6;

    // from interface AVRGameService
    public function startQuest (arg1 :Client, arg2 :String, arg3 :String, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, START_QUEST, [
            arg2, arg3, listener4
        ]);
    }

    /** The method id used to dispatch <code>updateQuest</code> requests. */
    public static const UPDATE_QUEST :int = 7;

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
