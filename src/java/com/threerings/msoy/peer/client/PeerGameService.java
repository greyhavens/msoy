//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides game-related peer services.
 */
public interface PeerGameService extends InvocationService
{
    /**
     * Reports to the supplied member that they have earned the specified amount of flow.
     */
    public void reportFlowAward (Client client, int memberId, int deltaFlow);
}
