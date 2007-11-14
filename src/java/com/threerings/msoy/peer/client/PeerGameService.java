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
    public void peerReportFlowAward (Client client, int memberId, int deltaFlow);

    /**
     * Notes that a player is either lobbying for, playing or no longer playing the specified game.
     */
    public void peerUpdatePlayer (Client client, int playerId, GameSummary game);

    /**
     * Notifies us that a player has persistently left their AVRG.
     */
    public void peerLeaveAVRGame (Client caller, int playerId);

    /**
     * Notifies us that a GameRecord has been modified for a game hosted on one of our game servers
     */
    public void gameRecordUpdated (Client caller, int gameId);
}
