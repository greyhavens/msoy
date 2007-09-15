//
// $Id$

package com.threerings.msoy.swiftly.data;

import com.threerings.msoy.swiftly.client.SwiftlyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

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
    public static final int ENTER_PROJECT = 1;

    // from interface SwiftlyService
    public void enterProject (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ENTER_PROJECT, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
