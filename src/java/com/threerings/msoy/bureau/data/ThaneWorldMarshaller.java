//
// $Id$

package com.threerings.msoy.bureau.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.bureau.client.ThaneWorldService;

/**
 * Provides the implementation of the {@link ThaneWorldService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from ThaneWorldService.java.")
public class ThaneWorldMarshaller extends InvocationMarshaller<ClientObject>
    implements ThaneWorldService
{
    /** The method id used to dispatch {@link #locateRoom} requests. */
    public static final int LOCATE_ROOM = 1;

    // from interface ThaneWorldService
    public void locateRoom (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(LOCATE_ROOM, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }
}
