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
     * Awards the given amount of flow to the given player. Awarded flow accumulates
     * server-side and is paid out when the game ends or when the player leaves the
     * game. If the amount exceeds the return value of {@link #getAvailableFlow}, it
     * is silently capped at that level.
     */
    void awardFlow (Client client, int playerId, int amount, InvocationListener listener);
}
