//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java PartyBoardService_JoinListener interface.
 */
public interface PartyBoardService_JoinListener
    extends InvocationService_InvocationListener
{
    // from Java PartyBoardService_JoinListener
    function foundParty (arg1 :int, arg2 :String, arg3 :int) :void

    // from Java PartyBoardService_JoinListener
    function priceUpdated (arg1 :PriceQuote) :void
}
}
