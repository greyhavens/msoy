//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides services for flash msoy games.
 */
public interface FlashGameService extends InvocationService
{
    /**
     * Request to end the turn, possibly futzing the next turn holder unless
     * -1 is specified for the nextPlayerIndex.
     */
    public void endTurn (
        Client client, int nextPlayerIndex, InvocationListener listener);

    /**
     * Request to end the game, with the specified player indices assigned
     * as winners.
     */
    public void endGame (
        Client client, int[] winners, InvocationListener listener);
}
