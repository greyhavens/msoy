//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.util.ActionScript;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.data.all.VizMemberName;

@ActionScript(omit=true)
public interface PeerPartyService extends InvocationService
{
    /**
     * Get party detail across nodes.
     */
    void getPartyDetail (Client client, int partyId, ResultListener rl);

    /**
     * Forward a request to join a party.
     */
    void joinParty (
        Client client, int partyId, VizMemberName player, byte groupRank, boolean hasLeaderInvite,
        ResultListener rl);

    /**
     * Get the scene for a party.
     */
    void getPartyScene (Client client, int partyId, ResultListener rl);
}
