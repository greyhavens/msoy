//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

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
    }

    /**
     * Retrieve a list of parties. Replies with a List<PartyBoardInfo>.
     */
    void getPartyBoard (Client client, String query, ResultListener rl);

    /**
     * Locates the specified party in the wide-Whirled.
     */
    void locateParty (Client client, int partyId, JoinListener jl);

    /**
     * Creates a new party with the requester as its leader.
     */
    void createParty (Client client, String name, int groupId, boolean inviteAllFriends,
                      JoinListener jl);

    /**
     * Retrieve detailed information on a party. Replies with a PartyDetail object.
     */
    void getPartyDetail (Client client, int partyId, ResultListener rl);
}
