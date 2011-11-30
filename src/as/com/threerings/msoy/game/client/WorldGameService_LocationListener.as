//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.langBoolean;

import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java WorldGameService_LocationListener interface.
 */
public interface WorldGameService_LocationListener
    extends InvocationService_InvocationListener
{
    // from Java WorldGameService_LocationListener
    function gameLocated (arg1 :String, arg2 :int, arg3 :Boolean) :void
}
}
