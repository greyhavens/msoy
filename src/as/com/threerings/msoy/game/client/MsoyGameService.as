//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;
import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java MsoyGameService interface.
 */
public interface MsoyGameService extends InvocationService
{
    // from Java interface MsoyGameService
    function awardFlow (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void;
}
}
