//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.util.Integer;

/**
 * An ActionScript version of the Java AVRGameService interface.
 */
public interface AVRGameService extends InvocationService
{
    // from Java interface AVRGameService
    function completeTask (arg1 :Client, arg2 :int, arg3 :String, arg4 :Number, arg5 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function loadOfflinePlayer (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface AVRGameService
    function setOfflinePlayerProperty (arg1 :Client, arg2 :int, arg3 :String, arg4 :Object, arg5 :Integer, arg6 :Boolean, arg7 :InvocationService_ConfirmListener) :void;
}
}
