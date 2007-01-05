//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;
import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java WorldGameService interface.
 */
public interface WorldGameService extends InvocationService
{
    // from Java interface WorldGameService
    function joinWorldGame (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void;
}
}
