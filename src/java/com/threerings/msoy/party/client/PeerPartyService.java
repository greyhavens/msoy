//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.util.ActionScript;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

/**
 * Provides party services that are needed between peers.
 */
@ActionScript(omit=true)
public interface PeerPartyService extends InvocationService<ClientObject>
{
    /**
     * Get party detail across nodes.
     */
    void getPartyDetail (int partyId, ResultListener rl);
}
