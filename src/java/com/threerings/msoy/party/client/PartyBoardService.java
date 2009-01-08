//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

public interface PartyBoardService extends InvocationService
{
    /** Provides a response to {@link #joinParty}. */
    public static interface JoinListener extends InvocationListener
    {
        /**
         * Reports the connection info for the Whirled node that is hosting the requested party.
         */
        void foundParty (String hostname, int port);
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
     * Create your own party. Replies with an oid, of the party object on success.
     */
    void createParty (Client client, String name, int groupId, boolean inviteAllFriends,
                      ResultListener rl);

    /**
     * Retrieve detailed information on a party. Replies with a PartyDetail object.
     */
    void getPartyDetail (Client client, int partyId, ResultListener rl);

    /**
     * Join the specified party. Replies with the (Integer) sceneId the party object on success.
     *
     * NOTE: going away soon.
     */
    void joinParty (Client client, int partyId, ResultListener rl);

    /**
     * If the client has a partyId set, it may call this method to learn about the oid of the party
     * object (if on the right server), or the sceneId (if not).  Returns an int[] {oid}, or an
     * Integer(sceneId). So sue me.
     *
     * NOTE: going away soon.
     */
    void locateMyParty (Client client, ResultListener rl);
}
