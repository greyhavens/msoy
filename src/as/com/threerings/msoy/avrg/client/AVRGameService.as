//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java AVRGameService interface.
 */
public interface AVRGameService extends InvocationService
{
    // from Java interface AVRGameService
    function cancelQuest (arg1 :Client, arg2 :String, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function completeQuest (arg1 :Client, arg2 :String, arg3 :Number, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function setTicker (arg1 :Client, arg2 :String, arg3 :int, arg4 :InvocationService_InvocationListener) :void;

    // from Java interface AVRGameService
    function startQuest (arg1 :Client, arg2 :String, arg3 :String, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function updateQuest (arg1 :Client, arg2 :String, arg3 :int, arg4 :String, arg5 :InvocationService_ConfirmListener) :void;
}
}
