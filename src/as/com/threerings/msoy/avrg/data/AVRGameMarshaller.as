//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.msoy.avrg.client.AVRGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Float;
import com.threerings.util.Integer;
import com.threerings.util.langBoolean;

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
    public function completeTask (arg1 :Client, arg2 :int, arg3 :String, arg4 :Number, arg5 :InvocationService_ConfirmListener) :void
    {
        var listener5 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, COMPLETE_TASK, [
            Integer.valueOf(arg2), arg3, Float.valueOf(arg4), listener5
        ]);
    }

    /** The method id used to dispatch <code>loadOfflinePlayer</code> requests. */
    public static const LOAD_OFFLINE_PLAYER :int = 2;

    // from interface AVRGameService
    public function loadOfflinePlayer (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOAD_OFFLINE_PLAYER, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>setOfflinePlayerProperty</code> requests. */
    public static const SET_OFFLINE_PLAYER_PROPERTY :int = 3;

    // from interface AVRGameService
    public function setOfflinePlayerProperty (arg1 :Client, arg2 :int, arg3 :String, arg4 :Object, arg5 :Integer, arg6 :Boolean, arg7 :InvocationService_ConfirmListener) :void
    {
        var listener7 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, SET_OFFLINE_PLAYER_PROPERTY, [
            Integer.valueOf(arg2), arg3, arg4, arg5, langBoolean.valueOf(arg6), listener7
        ]);
    }
}
}
