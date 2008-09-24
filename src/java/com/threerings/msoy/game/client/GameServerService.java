//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.stats.data.StatModifier;

import com.threerings.msoy.data.UserAction;
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
    void sayHello (Client client, int port);

    /**
     * Notes that a player is either lobbying for, playing or no longer playing the specified game.
     */
    void updatePlayer (Client client, int playerId, GameSummary game);

    /**
     * Notifies us that a player has persistently left their AVRG.
     */
    void leaveAVRGame (Client caller, int playerId);

    /**
     * Reports that the game server on the specified port is no longer hosting the specified game.
     */
    void clearGameHost (Client client, int port, int gameId);

    /**
     * Reports an intermediate coin award made by a game to a player.
     */
    void reportCoinAward (Client client, int memberId, int deltaCoins);

    /**
     * Indicates some coins should be awarded to the specified player.
     */
    void awardCoins (Client client, int gameId, UserAction action, int amount);

    /**
     * Reports a trophy award made by a game to a player.
     */
    void reportTrophyAward (Client client, int memberId, String gameName, Trophy trophy);

    /**
     * Awards the specified prize to the specified player. Returns the awarded {@link Item} to the
     * caller via the supplied result listener.
     */
    void awardPrize (Client client, int memberId, int gameId, String gameName, Prize prize,
                     ResultListener listener);

    /**
     * Applies a stat update for the specified player.
     */
    void updateStat (Client client, int memberId, StatModifier<?> modifier);
}
