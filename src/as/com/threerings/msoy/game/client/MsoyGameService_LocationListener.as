//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java MsoyGameService_LocationListener interface.
 */
public interface MsoyGameService_LocationListener
    extends InvocationService_InvocationListener
{
    // from Java MsoyGameService_LocationListener
    function gameLocated (arg1 :String, arg2 :int) :void
}
}
