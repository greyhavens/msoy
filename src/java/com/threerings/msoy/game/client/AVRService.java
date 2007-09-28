//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * A service for managing AVR (in-world) game life cycles.
 */
public interface AVRService extends InvocationService
{
    /**
     * Requests to active an AVR Game.
     *
     * @param gameId the item id of a Game-type item.
     * @param listener a listener to return result to or notify on failure.
     */
    public void activateGame (Client client, int gameId, ResultListener listener);
}
