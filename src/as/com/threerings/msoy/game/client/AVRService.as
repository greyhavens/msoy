//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.game.client.AVRService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * An ActionScript version of the Java AVRService interface.
 */
public interface AVRService extends InvocationService
{
    // from Java interface AVRService
    function activateGame (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface AVRService
    function deactivateGame (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void;
}
}
