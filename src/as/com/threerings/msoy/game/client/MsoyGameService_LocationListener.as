//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.
import com.threerings.io.TypedArray;

import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.client.MsoyGameService_LocationListener;
import com.threerings.msoy.game.data.MsoyGameMarshaller_LocationMarshaller;
import com.threerings.presents.client.Client;
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
