//
// $Id$

package com.threerings.msoy.swiftly.data {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.

import com.threerings.msoy.swiftly.client.SwiftlyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * Provides the implementation of the {@link SwiftlyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class SwiftlyMarshaller extends InvocationMarshaller
    implements SwiftlyService
{
    /** The method id used to dispatch {@link #enterProject} requests. */
    public static const ENTER_PROJECT :int = 1;

    // from interface SwiftlyService
    public function enterProject (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ENTER_PROJECT, [
            Integer.valueOf(arg2), listener3
        ]);
    }
}
}
