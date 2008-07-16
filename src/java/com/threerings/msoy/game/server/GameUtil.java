//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.StringUtil;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.xml.MsoyGameParser;
import com.threerings.msoy.item.data.all.Game;

import static com.threerings.msoy.Log.log;

/**
 * Contains game-related utility methods.
 */
public class GameUtil
{
    /**
     * Returns the minimum and maximum players for the supplied game.
     */
    public static int[] getMinMaxPlayers (Game game)
    {
        MsoyMatchConfig match = null;
        try {
            if (game != null && !StringUtil.isBlank(game.config)) {
                match = (MsoyMatchConfig)new MsoyGameParser().parseGame(game).match;
            }
            if (match == null) {
                log.warning("Game missing match configuration [game=" + game + "].");
            }
        } catch (Exception e) {
            log.warning("Failed to parse XML game definition [id=" + game.gameId +
                    ", config=" + game.config + "]", e);
        }
        if (match != null) {
            return new int[] {
                match.minSeats,
                (match.getMatchType() == GameConfig.PARTY) ? Integer.MAX_VALUE : match.maxSeats
            };
        }
        return new int[] { 1, 2 }; // arbitrary defaults
    }
}
