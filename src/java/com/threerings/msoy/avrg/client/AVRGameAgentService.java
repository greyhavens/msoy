//
// $Id$

package com.threerings.msoy.avrg.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

public interface AVRGameAgentService extends InvocationService
{
    /**
     * Ends a player's participation in the AVRG. This is required here because agents do not have 
     * access to any other location management services.
     */
    void leaveGame (Client caller, int playerId);
}
