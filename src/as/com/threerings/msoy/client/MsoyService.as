//
// $Id$

package com.threerings.msoy.client {

import com.threerings.io.TypedArray;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java MsoyService interface.
 */
public interface MsoyService extends InvocationService
{
    // from Java interface MsoyService
    function dispatchDeferredNotifications () :void;

    // from Java interface MsoyService
    function emailShare (arg1 :Boolean, arg2 :String, arg3 :int, arg4 :TypedArray /* of class java.lang.String */, arg5 :String, arg6 :Boolean, arg7 :InvocationService_ConfirmListener) :void;

    // from Java interface MsoyService
    function getABTestGroup (arg1 :String, arg2 :Boolean, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MsoyService
    function purchaseAndSendBroadcast (arg1 :int, arg2 :String, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MsoyService
    function secureBroadcastQuote (arg1 :InvocationService_ResultListener) :void;

    // from Java interface MsoyService
    function setHearingGroupChat (arg1 :int, arg2 :Boolean, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MsoyService
    function trackTestAction (arg1 :String, arg2 :String) :void;
}
}
