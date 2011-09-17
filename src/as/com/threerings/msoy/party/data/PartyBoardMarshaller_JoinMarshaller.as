//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.party.client.PartyBoardService_JoinListener;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;

/**
 * Marshalls instances of the PartyBoardService_JoinMarshaller interface.
 */
public class PartyBoardMarshaller_JoinMarshaller
    extends InvocationMarshaller_ListenerMarshaller
{
    /** The method id used to dispatch <code>foundParty</code> responses. */
    public static const FOUND_PARTY :int = 1;

    /** The method id used to dispatch <code>priceUpdated</code> responses. */
    public static const PRICE_UPDATED :int = 2;

    // from InvocationMarshaller_ListenerMarshaller
    override public function dispatchResponse (methodId :int, args :Array) :void
    {
        switch (methodId) {
        case FOUND_PARTY:
            (listener as PartyBoardService_JoinListener).foundParty(
                (args[0] as int), (args[1] as String), (args[2] as int));
            return;

        case PRICE_UPDATED:
            (listener as PartyBoardService_JoinListener).priceUpdated(
                (args[0] as PriceQuote));
            return;

        default:
            super.dispatchResponse(methodId, args);
            return;
        }
    }
}
}
