//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.msoy.world.client.WatcherService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>WatcherService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class WatcherMarshaller extends InvocationMarshaller
    implements WatcherService
{
    /** The method id used to dispatch <code>addWatch</code> requests. */
    public static const ADD_WATCH :int = 1;

    // from interface WatcherService
    public function addWatch (arg1 :Client, arg2 :int) :void
    {
        sendRequest(arg1, ADD_WATCH, [
            Integer.valueOf(arg2)
        ]);
    }

    /** The method id used to dispatch <code>clearWatch</code> requests. */
    public static const CLEAR_WATCH :int = 2;

    // from interface WatcherService
    public function clearWatch (arg1 :Client, arg2 :int) :void
    {
        sendRequest(arg1, CLEAR_WATCH, [
            Integer.valueOf(arg2)
        ]);
    }
}
}
