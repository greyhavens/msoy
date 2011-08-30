//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;

/**
 * An ActionScript version of the Java AVRService interface.
 */
public interface AVRService extends InvocationService
{
    // from Java interface AVRService
    function activateGame (arg1 :int, arg2 :AVRService_AVRGameJoinListener) :void;

    // from Java interface AVRService
    function deactivateGame (arg1 :int, arg2 :InvocationService_ConfirmListener) :void;
}
}
