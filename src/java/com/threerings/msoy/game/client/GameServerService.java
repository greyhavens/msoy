//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.game.data.GameSummary;

/**
 * Invocation services called by our external game servers.
 */
public interface GameServerService extends InvocationService
{
    /**
     * Lets our world server know who we are so that it can send us messages as desired.
     */
    public void sayHello (Client client, int port);

    /**
     * Notes that a player is either lobbying for, playing or no longer playing the specified game.
     */
    public void updatePlayer (Client client, int playerId, GameSummary game);

    /**
     * Reports that the game server on the specified port is no longer hosting the specified game.
     */
    public void clearGameHost (Client client, int port, int gameId);

    /**
     * Reports an intermediate flow award made by a game to a player.
     */
    public void reportFlowAward (Client client, int memberId, int deltaFlow);
}
