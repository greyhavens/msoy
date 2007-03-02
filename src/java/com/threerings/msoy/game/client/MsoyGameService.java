//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * A service for querying and awarding flow for games.
 */
public interface MsoyGameService extends InvocationService
{
    /**
     * Awards the given amount of flow to the 'this' occupant. Awarded flow accumulates
     * server-side and is paid out when the game ends or when the player leaves the
     * game. If the amount exceeds the server-calculated cap, it is silently
     * capped at that level.
     */
    void awardFlow (Client client, int amount, InvocationListener listener);
}
