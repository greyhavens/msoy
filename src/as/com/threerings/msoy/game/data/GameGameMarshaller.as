//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.msoy.game.client.GameGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>GameGameService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class GameGameMarshaller extends InvocationMarshaller
    implements GameGameService
{
    /** The method id used to dispatch <code>getTrophies</code> requests. */
    public static const GET_TROPHIES :int = 1;

    // from interface GameGameService
    public function getTrophies (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_TROPHIES, [
            Integer.valueOf(arg2), listener3
        ]);
    }
}
}
