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
     * Reports to the supplied member that they have earned the specified amount of flow.
     */
    public void reportFlowAward (Client client, int memberId, int deltaFlow);

    /**
     * Notes that a player is either lobbying for, playing or no longer playing the specified game.
     */
    public void updatePlayer (Client client, int playerId, GameSummary game);

    /**
     * Notifies us that a player has persistently left their AVRG.
     */
    public void leaveAVRGame (Client caller, int playerId);
}
