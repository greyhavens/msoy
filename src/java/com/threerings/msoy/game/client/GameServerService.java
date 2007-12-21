//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Prize;

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
     * Notifies us that a player has persistently left their AVRG.
     */
    public void leaveAVRGame (Client caller, int playerId);

    /**
     * Reports that the game server on the specified port is no longer hosting the specified game.
     */
    public void clearGameHost (Client client, int port, int gameId);

    /**
     * Reports an intermediate flow award made by a game to a player.
     */
    public void reportFlowAward (Client client, int memberId, int deltaFlow);

    /**
     * Reports a trophy award made by a game to a player.
     */
    public void reportTrophyAward (Client client, int memberId, String gameName, Trophy trophy);

    /**
     * Awards the specified prize to the specified player. Returns the awarded {@link Item} to the
     * caller via the supplied result listener.
     */
    public void awardPrize (Client client, int memberId, int gameId, String gameName, Prize prize,
                            ResultListener listener);
}
