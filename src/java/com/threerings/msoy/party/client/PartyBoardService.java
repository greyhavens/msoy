//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

/**
 * Provides party services accessed via a world session.
 */
public interface PartyBoardService extends InvocationService
{
    /** Provides a response to {@link #createParty} and {@link #locateParty}. */
    public static interface JoinListener extends InvocationListener
    {
        /**
         * Reports the connection info for the Whirled node that is hosting the requested party.
         */
        void foundParty (int partyId, String hostname, int port);

        /**
         * Whoops, the price was updated.
         */
        void priceUpdated (PriceQuote newQuote);
    }

    /**
     * Retrieve a list of parties. Replies with a List<PartyBoardInfo>.
     */
    void getPartyBoard (Client client, byte mode, ResultListener rl);

    /**
     * Locates the specified party in the wide-Whirled.
     */
    void locateParty (Client client, int partyId, JoinListener jl);

    /**
     * Get the cost of creating a party. Replies with a PriceQuote.
     */
    void getCreateCost (Client client, ResultListener rl);

    /**
     * Creates a new party with the requester as its leader.
     */
    void createParty (
        Client client, Currency currency, int authedCost,
        String name, int groupId, boolean inviteAllFriends, JoinListener jl);

    /**
     * Retrieve detailed information on a party. Replies with a PartyDetail object.
     */
    void getPartyDetail (Client client, int partyId, ResultListener rl);
}
