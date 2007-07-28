//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.
import com.threerings.io.TypedArray;

import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.client.MsoyGameService_LocationListener;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;

/**
 * Marshalls instances of the MsoyGameService_LocationMarshaller interface.
 */
public class MsoyGameMarshaller_LocationMarshaller
    extends InvocationMarshaller_ListenerMarshaller
{
    /** The method id used to dispatch {@link #gameLocated} responses. */
    public static const GAME_LOCATED :int = 1;

    // from InvocationMarshaller_ListenerMarshaller
    override public function dispatchResponse (methodId :int, args :Array) :void
    {
        switch (methodId) {
        case GAME_LOCATED:
            (listener as MsoyGameService_LocationListener).gameLocated(
                (args[0] as String), (args[1] as int));
            return;

        default:
            super.dispatchResponse(methodId, args);
            return;
        }
    }
}
}
