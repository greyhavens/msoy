//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

public interface PartyBoardService extends InvocationService
{
    /**
     * Retrieve a list of parties.
     * @returns an array of PartyInfo objects.
     */
    void getPartyBoard (Client client, String query, ResultListener rl);

    /**
     * Join the specified party.
     */
    void joinParty (Client client, int partyId, ResultListener rl);

    /**
     * Create your own party.
     */
    void createParty (Client client, String name, int groupId, ResultListener rl);
}
