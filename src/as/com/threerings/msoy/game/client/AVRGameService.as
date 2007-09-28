//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.game.client.AVRGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;

/**
 * An ActionScript version of the Java AVRGameService interface.
 */
public interface AVRGameService extends InvocationService
{
    // from Java interface AVRGameService
    function completeQuest (arg1 :Client, arg2 :String, arg3 :int, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function deletePlayerProperty (arg1 :Client, arg2 :String, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function deleteProperty (arg1 :Client, arg2 :String, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function setPlayerProperty (arg1 :Client, arg2 :String, arg3 :ByteArray, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function setProperty (arg1 :Client, arg2 :String, arg3 :ByteArray, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function startQuest (arg1 :Client, arg2 :String, arg3 :String, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface AVRGameService
    function updateQuest (arg1 :Client, arg2 :String, arg3 :int, arg4 :String, arg5 :InvocationService_ConfirmListener) :void;
}
}
