//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.world.data.MemoryEntry;

/**
 * A service for joining AVR (in-world) games.
 */
public interface AVRGameService extends InvocationService
{
    /**
     * Requests to join an in-world game.
     *
     * @param gameId the item id of a Game-type item.
     * @param listener a listener to notify on failure.
     */
    public void joinAVRGame (Client client, int gameId, InvocationListener listener);

    /**
     * Requests to leave the current in-world game.
     *
     * @param listener a listener to notify on failure.
     */
    public void leaveAVRGame (Client client, InvocationListener listener);

    /**
     * Requests to change the game memory.
     */
    public void updateMemory (Client client, MemoryEntry entry);
}
