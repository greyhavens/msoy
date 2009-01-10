//
// $Id$

package com.threerings.msoy.party.data {

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

    // from InvocationMarshaller_ListenerMarshaller
    override public function dispatchResponse (methodId :int, args :Array) :void
    {
        switch (methodId) {
        case FOUND_PARTY:
            (listener as PartyBoardService_JoinListener).foundParty(
                (args[0] as int), (args[1] as String), (args[2] as int));
            return;

        default:
            super.dispatchResponse(methodId, args);
            return;
        }
    }
}
}
