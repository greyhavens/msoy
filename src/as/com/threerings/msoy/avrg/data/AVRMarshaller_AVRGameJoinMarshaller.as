//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.msoy.avrg.client.AVRService_AVRGameJoinListener;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;

/**
 * Marshalls instances of the AVRService_AVRGameJoinMarshaller interface.
 */
public class AVRMarshaller_AVRGameJoinMarshaller
    extends InvocationMarshaller_ListenerMarshaller
{
    /** The method id used to dispatch <code>avrgJoined</code> responses. */
    public static const AVRG_JOINED :int = 1;

    // from InvocationMarshaller_ListenerMarshaller
    override public function dispatchResponse (methodId :int, args :Array) :void
    {
        switch (methodId) {
        case AVRG_JOINED:
            (listener as AVRService_AVRGameJoinListener).avrgJoined(
                (args[0] as int), (args[1] as AVRGameConfig));
            return;

        default:
            super.dispatchResponse(methodId, args);
            return;
        }
    }
}
}
