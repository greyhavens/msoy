//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.util.ActionScript;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides party services that are needed between peers.
 */
@ActionScript(omit=true)
public interface PeerPartyService extends InvocationService
{
    /**
     * Get party detail across nodes.
     */
    void getPartyDetail (Client client, int partyId, ResultListener rl);
}
