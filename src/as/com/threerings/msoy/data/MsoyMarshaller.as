//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.TypedArray;
import com.threerings.msoy.client.MsoyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Integer;
import com.threerings.util.langBoolean;

/**
 * Provides the implementation of the <code>MsoyService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MsoyMarshaller extends InvocationMarshaller
    implements MsoyService
{
    /** The method id used to dispatch <code>dispatchDeferredNotifications</code> requests. */
    public static const DISPATCH_DEFERRED_NOTIFICATIONS :int = 1;

    // from interface MsoyService
    public function dispatchDeferredNotifications () :void
    {
        sendRequest(DISPATCH_DEFERRED_NOTIFICATIONS, [
            
        ]);
    }

    /** The method id used to dispatch <code>emailShare</code> requests. */
    public static const EMAIL_SHARE :int = 2;

    // from interface MsoyService
    public function emailShare (arg1 :Boolean, arg2 :String, arg3 :int, arg4 :TypedArray /* of class java.lang.String */, arg5 :String, arg6 :Boolean, arg7 :InvocationService_ConfirmListener) :void
    {
        var listener7 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(EMAIL_SHARE, [
            langBoolean.valueOf(arg1), arg2, Integer.valueOf(arg3), arg4, arg5, langBoolean.valueOf(arg6), listener7
        ]);
    }

    /** The method id used to dispatch <code>getABTestGroup</code> requests. */
    public static const GET_ABTEST_GROUP :int = 3;

    // from interface MsoyService
    public function getABTestGroup (arg1 :String, arg2 :Boolean, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(GET_ABTEST_GROUP, [
            arg1, langBoolean.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>purchaseAndSendBroadcast</code> requests. */
    public static const PURCHASE_AND_SEND_BROADCAST :int = 4;

    // from interface MsoyService
    public function purchaseAndSendBroadcast (arg1 :int, arg2 :String, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(PURCHASE_AND_SEND_BROADCAST, [
            Integer.valueOf(arg1), arg2, listener3
        ]);
    }

    /** The method id used to dispatch <code>secureBroadcastQuote</code> requests. */
    public static const SECURE_BROADCAST_QUOTE :int = 5;

    // from interface MsoyService
    public function secureBroadcastQuote (arg1 :InvocationService_ResultListener) :void
    {
        var listener1 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(SECURE_BROADCAST_QUOTE, [
            listener1
        ]);
    }

    /** The method id used to dispatch <code>setHearingGroupChat</code> requests. */
    public static const SET_HEARING_GROUP_CHAT :int = 6;

    // from interface MsoyService
    public function setHearingGroupChat (arg1 :int, arg2 :Boolean, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(SET_HEARING_GROUP_CHAT, [
            Integer.valueOf(arg1), langBoolean.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>trackTestAction</code> requests. */
    public static const TRACK_TEST_ACTION :int = 7;

    // from interface MsoyService
    public function trackTestAction (arg1 :String, arg2 :String) :void
    {
        sendRequest(TRACK_TEST_ACTION, [
            arg1, arg2
        ]);
    }
}
}
