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
     * Returns the amount of flow that could be awarded to the given player right now.
     * This amount slowly accumulates over the course of the game, depending on a global
     * flow-per-minute value, each player's humanity factor, and the current anti-abuse
     * factor for the game itself. 
     */
    void getAvailableFlow (Client client, int playerId, ResultListener listener);
    
    /**
     * Awards the given amount of flow to the given player. Awarded flow accumulates
     * server-side and is paid out when the game ends or when the player leaves the
     * game. If the amount exceeds the return value of {@link #getAvailableFlow}, it
     * is silently capped at that level.
     */
    void awardFlow (Client client, int playerId, int amount, InvocationListener listener);
}
