//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.TypedArray;

import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;

import com.threerings.msoy.world.client.WorldService_HomeResultListener;

/**
 * Marshalls instances of the WorldService_HomeResultMarshaller interface.
 */
public class WorldMarshaller_HomeResultMarshaller
    extends InvocationMarshaller_ListenerMarshaller
{
    /** The method id used to dispatch <code>readyToEnter</code> responses. */
    public static const READY_TO_ENTER :int = 1;

    /** The method id used to dispatch <code>selectGift</code> responses. */
    public static const SELECT_GIFT :int = 2;

    // from InvocationMarshaller_ListenerMarshaller
    override public function dispatchResponse (methodId :int, args :Array) :void
    {
        switch (methodId) {
        case READY_TO_ENTER:
            (listener as WorldService_HomeResultListener).readyToEnter(
                (args[0] as int));
            return;

        case SELECT_GIFT:
            (listener as WorldService_HomeResultListener).selectGift(
                (args[0] as TypedArray /* of class com.threerings.msoy.item.data.all.Avatar */), (args[1] as int));
            return;

        default:
            super.dispatchResponse(methodId, args);
            return;
        }
    }
}
}
