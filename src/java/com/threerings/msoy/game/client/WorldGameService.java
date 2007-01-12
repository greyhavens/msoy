//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.world.data.MemoryEntry;

/**
 * A service for joining in-world games.
 */
public interface WorldGameService extends InvocationService
{
    /**
     * Requests to join an in-world game.
     *
     * @param gameId the item id of a Game-type item.
     * @param listener a listener to notify on failure
     */
    public void joinWorldGame (Client client, int gameId, InvocationListener listener);
    
    /**
     * Requests to leave the current in-world game.
     *
     * @param listener a listener to notify on failure
     */
    public void leaveWorldGame (Client client, InvocationListener listener);
    
    /**
     * Requests to change the game memory.
     */
    public void updateMemory (Client client, MemoryEntry entry);
}
