//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.util.Float;
import com.threerings.util.Integer;
import com.threerings.util.langBoolean;

import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

import com.threerings.msoy.avrg.client.AVRGameService;
import com.threerings.msoy.room.data.MsoyLocation;

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
    /** The method id used to dispatch <code>completeTask</code> requests. */
    public static const COMPLETE_TASK :int = 1;

    // from interface AVRGameService
    public function completeTask (arg1 :int, arg2 :String, arg3 :Number, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(COMPLETE_TASK, [
            Integer.valueOf(arg1), arg2, Float.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>loadOfflinePlayer</code> requests. */
    public static const LOAD_OFFLINE_PLAYER :int = 2;

    // from interface AVRGameService
    public function loadOfflinePlayer (arg1 :int, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(LOAD_OFFLINE_PLAYER, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>movePlayerToRoom</code> requests. */
    public static const MOVE_PLAYER_TO_ROOM :int = 3;

    // from interface AVRGameService
    public function movePlayerToRoom (arg1 :int, arg2 :int, arg3 :MsoyLocation, arg4 :InvocationService_InvocationListener) :void
    {
        var listener4 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(MOVE_PLAYER_TO_ROOM, [
            Integer.valueOf(arg1), Integer.valueOf(arg2), arg3, listener4
        ]);
    }

    /** The method id used to dispatch <code>setIdle</code> requests. */
    public static const SET_IDLE :int = 4;

    // from interface AVRGameService
    public function setIdle (arg1 :Boolean, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(SET_IDLE, [
            langBoolean.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>setOfflinePlayerProperty</code> requests. */
    public static const SET_OFFLINE_PLAYER_PROPERTY :int = 5;

    // from interface AVRGameService
    public function setOfflinePlayerProperty (arg1 :int, arg2 :String, arg3 :Object, arg4 :Integer, arg5 :Boolean, arg6 :InvocationService_ConfirmListener) :void
    {
        var listener6 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(SET_OFFLINE_PLAYER_PROPERTY, [
            Integer.valueOf(arg1), arg2, arg3, arg4, langBoolean.valueOf(arg5), listener6
        ]);
    }
}
}
