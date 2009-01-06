//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Bootstrap game services provided by the Game server.
 */
public interface GameGameService extends InvocationService
{
    /**
     * Returns a list of all trophies awarded by the specified game along with information on when
     * the caller earned those trophies, if they've earned them.
     */
    void getTrophies (Client client, int gameId, ResultListener listener);
}
