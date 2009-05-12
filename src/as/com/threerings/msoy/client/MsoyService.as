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
    function dispatchDeferredNotifications (arg1 :Client) :void;

    // from Java interface MsoyService
    function emailShare (arg1 :Client, arg2 :Boolean, arg3 :String, arg4 :int, arg5 :TypedArray /* of class java.lang.String */, arg6 :String, arg7 :Boolean, arg8 :InvocationService_ConfirmListener) :void;

    // from Java interface MsoyService
    function getABTestGroup (arg1 :Client, arg2 :String, arg3 :Boolean, arg4 :InvocationService_ResultListener) :void;

    // from Java interface MsoyService
    function purchaseAndSendBroadcast (arg1 :Client, arg2 :int, arg3 :String, arg4 :InvocationService_ResultListener) :void;

    // from Java interface MsoyService
    function secureBroadcastQuote (arg1 :Client, arg2 :InvocationService_ResultListener) :void;

    // from Java interface MsoyService
    function setHearingGroupChat (arg1 :Client, arg2 :int, arg3 :Boolean, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface MsoyService
    function trackClientAction (arg1 :Client, arg2 :String, arg3 :String) :void;

    // from Java interface MsoyService
    function trackTestAction (arg1 :Client, arg2 :String, arg3 :String) :void;

    // from Java interface MsoyService
    function trackVectorAssociation (arg1 :Client, arg2 :String) :void;
}
}
