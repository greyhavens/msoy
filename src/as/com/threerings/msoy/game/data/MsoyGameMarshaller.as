//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.
import com.threerings.io.TypedArray;

import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.client.MsoyGameService_LocationListener;
import com.threerings.msoy.game.data.MsoyGameMarshaller_LocationMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;

/**
 * Provides the implementation of the {@link MsoyGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MsoyGameMarshaller extends InvocationMarshaller
    implements MsoyGameService
{
    /** The method id used to dispatch {@link #locateGame} requests. */
    public static const LOCATE_GAME :int = 1;

    // from interface MsoyGameService
    public function locateGame (arg1 :Client, arg2 :int, arg3 :MsoyGameService_LocationListener) :void
    {
        var listener3 :MsoyGameMarshaller_LocationMarshaller = new MsoyGameMarshaller_LocationMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOCATE_GAME, [
            Integer.valueOf(arg2), listener3
        ]);
    }
}
}
