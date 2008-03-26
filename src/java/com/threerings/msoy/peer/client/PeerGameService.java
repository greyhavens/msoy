//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides game-related peer services.
 */
public interface PeerGameService extends InvocationService
{
    /**
     * Notifies us that a GameRecord has been modified for a game hosted on one of our game servers
     */
    public void gameRecordUpdated (Client caller, int gameId);
}
