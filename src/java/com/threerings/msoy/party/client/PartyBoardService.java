//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

public interface PartyBoardService extends InvocationService
{
    /**
     * Retrieve a list of parties.
     * Returns a List<PartyInfo>.
     */
    void getPartyBoard (Client client, String query, ResultListener rl);

    /**
     * Join the specified party.
     * Returns the (Integer) sceneId the party object on success.
     */
    void joinParty (Client client, int partyId, ResultListener rl);

    /**
     * Create your own party.
     * Returns an oid, of the party object on success.
     */
    void createParty (
        Client client, String name, int groupId, boolean inviteAllFriends, ResultListener rl);

    /**
     * If the client has a partyId set, it may call this method to learn about the oid
     * of the party object (if on the right server), or the sceneId (if not).
     * Returns an int[] {oid}, or an Integer(sceneId). So sue me.
     */
    void locateMyParty (Client client, ResultListener rl);

    /**
     * Retrieve detailed information on a party.
     * Returns a List<PartyPeep>.
     */
    void getPartyDetail (Client client, int partyId, ResultListener rl);
}
