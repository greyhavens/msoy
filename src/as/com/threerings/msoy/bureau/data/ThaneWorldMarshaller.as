//
// $Id$

package com.threerings.msoy.bureau.data {

import com.threerings.util.Integer;

import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

import com.threerings.msoy.bureau.client.ThaneWorldService;

/**
 * Provides the implementation of the <code>ThaneWorldService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class ThaneWorldMarshaller extends InvocationMarshaller
    implements ThaneWorldService
{
    /** The method id used to dispatch <code>locateRoom</code> requests. */
    public static const LOCATE_ROOM :int = 1;

    // from interface ThaneWorldService
    public function locateRoom (arg1 :int, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(LOCATE_ROOM, [
            Integer.valueOf(arg1), listener2
        ]);
    }
}
}
