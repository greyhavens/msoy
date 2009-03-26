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
    public function dispatchDeferredNotifications (arg1 :Client) :void
    {
        sendRequest(arg1, DISPATCH_DEFERRED_NOTIFICATIONS, [
            
        ]);
    }

    /** The method id used to dispatch <code>emailShare</code> requests. */
    public static const EMAIL_SHARE :int = 2;

    // from interface MsoyService
    public function emailShare (arg1 :Client, arg2 :Boolean, arg3 :String, arg4 :int, arg5 :TypedArray /* of class java.lang.String */, arg6 :String, arg7 :InvocationService_ConfirmListener) :void
    {
        var listener7 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, EMAIL_SHARE, [
            langBoolean.valueOf(arg2), arg3, Integer.valueOf(arg4), arg5, arg6, listener7
        ]);
    }

    /** The method id used to dispatch <code>getABTestGroup</code> requests. */
    public static const GET_ABTEST_GROUP :int = 3;

    // from interface MsoyService
    public function getABTestGroup (arg1 :Client, arg2 :String, arg3 :Boolean, arg4 :InvocationService_ResultListener) :void
    {
        var listener4 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, GET_ABTEST_GROUP, [
            arg2, langBoolean.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>setHearingGroupChat</code> requests. */
    public static const SET_HEARING_GROUP_CHAT :int = 4;

    // from interface MsoyService
    public function setHearingGroupChat (arg1 :Client, arg2 :int, arg3 :Boolean, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_HEARING_GROUP_CHAT, [
            Integer.valueOf(arg2), langBoolean.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>trackClientAction</code> requests. */
    public static const TRACK_CLIENT_ACTION :int = 5;

    // from interface MsoyService
    public function trackClientAction (arg1 :Client, arg2 :String, arg3 :String) :void
    {
        sendRequest(arg1, TRACK_CLIENT_ACTION, [
            arg2, arg3
        ]);
    }

    /** The method id used to dispatch <code>trackTestAction</code> requests. */
    public static const TRACK_TEST_ACTION :int = 6;

    // from interface MsoyService
    public function trackTestAction (arg1 :Client, arg2 :String, arg3 :String) :void
    {
        sendRequest(arg1, TRACK_TEST_ACTION, [
            arg2, arg3
        ]);
    }

    /** The method id used to dispatch <code>trackVectorAssociation</code> requests. */
    public static const TRACK_VECTOR_ASSOCIATION :int = 7;

    // from interface MsoyService
    public function trackVectorAssociation (arg1 :Client, arg2 :String) :void
    {
        sendRequest(arg1, TRACK_VECTOR_ASSOCIATION, [
            arg2
        ]);
    }
}
}
