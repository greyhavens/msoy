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
    void getTrophies (int gameId, ResultListener listener);

    /**
     * Removes the trophies for this player from this in-development game.
     */
    void removeDevelopmentTrophies (int gameId, ConfirmListener listener);
    
    /**
     * Posts a complaint event in underwire.
     */
    void complainPlayer (int memberId, String complaint);    
}
