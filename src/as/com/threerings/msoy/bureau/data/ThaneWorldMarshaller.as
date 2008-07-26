//
// $Id$

package com.threerings.msoy.bureau.data {

import com.threerings.msoy.bureau.client.ThaneWorldService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Integer;

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
    public static const LOCATE_ROOM :int = 1;

    // from interface ThaneWorldService
    public function locateRoom (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOCATE_ROOM, [
            Integer.valueOf(arg2), listener3
        ]);
    }
}
}
