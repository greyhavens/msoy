//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Float;
import com.threerings.util.Integer;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;

import com.threerings.msoy.room.data.MsoyLocation;

/**
 * An ActionScript version of the Java AVRGameService interface.
 */
public interface AVRGameService extends InvocationService
{
    // from Java interface AVRGameService
    function completeTask (arg1 :int, arg2 :String, arg3 :Number, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function loadOfflinePlayer (arg1 :int, arg2 :InvocationService_ResultListener) :void;

    // from Java interface AVRGameService
    function movePlayerToRoom (arg1 :int, arg2 :int, arg3 :MsoyLocation, arg4 :InvocationService_InvocationListener) :void;

    // from Java interface AVRGameService
    function setIdle (arg1 :Boolean, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function setOfflinePlayerProperty (arg1 :int, arg2 :String, arg3 :Object, arg4 :Integer, arg5 :Boolean, arg6 :InvocationService_ConfirmListener) :void;
}
}
