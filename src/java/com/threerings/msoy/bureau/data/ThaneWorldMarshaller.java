//
// $Id$

package com.threerings.msoy.bureau.data;

import com.threerings.msoy.bureau.client.ThaneWorldService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link ThaneWorldService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class ThaneWorldMarshaller extends InvocationMarshaller
    implements ThaneWorldService
{
    /** The method id used to dispatch {@link #locateRoom} requests. */
    public static final int LOCATE_ROOM = 1;

    // from interface ThaneWorldService
    public void locateRoom (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOCATE_ROOM, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
