//
// $Id$

package com.threerings.msoy.swiftly.client {

import flash.utils.ByteArray;
import com.threerings.msoy.swiftly.client.SwiftlyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * An ActionScript version of the Java SwiftlyService interface.
 */
public interface SwiftlyService extends InvocationService
{
    // from Java interface SwiftlyService
    function enterProject (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;
}
}
