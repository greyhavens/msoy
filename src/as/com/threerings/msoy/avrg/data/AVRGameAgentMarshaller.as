//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.msoy.avrg.client.AVRGameAgentService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>AVRGameAgentService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class AVRGameAgentMarshaller extends InvocationMarshaller
    implements AVRGameAgentService
{
    /** The method id used to dispatch <code>leaveGame</code> requests. */
    public static const LEAVE_GAME :int = 1;

    // from interface AVRGameAgentService
    public function leaveGame (arg1 :Client, arg2 :int) :void
    {
        sendRequest(arg1, LEAVE_GAME, [
            Integer.valueOf(arg2)
        ]);
    }
}
}
