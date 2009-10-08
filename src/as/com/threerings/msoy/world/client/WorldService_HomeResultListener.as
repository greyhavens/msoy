//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.io.TypedArray;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java WorldService_HomeResultListener interface.
 */
public interface WorldService_HomeResultListener
    extends InvocationService_InvocationListener
{
    // from Java WorldService_HomeResultListener
    function readyToEnter (arg1 :int) :void

    // from Java WorldService_HomeResultListener
    function selectGift (arg1 :TypedArray /* of class com.threerings.msoy.item.data.all.Avatar */, arg2 :int) :void
}
}
