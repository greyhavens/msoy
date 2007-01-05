//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

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
}
