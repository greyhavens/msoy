//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.StringUtil;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.gwt.GameCode;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.xml.MsoyGameParser;

import static com.threerings.msoy.Log.log;

/**
 * Contains game-related utility methods.
 */
public class GameUtil
{
    /**
     * Returns the minimum and maximum players for the supplied game.
     */
    public static int[] getMinMaxPlayers (GameInfo info, GameCode game)
    {
        // AVRGs are 1+
        if (info.isAVRG) {
            return new int[] { 1, Integer.MAX_VALUE };
        }

        MsoyMatchConfig match = null;
        try {
            if (game != null && !StringUtil.isBlank(game.config)) {
                match = (MsoyMatchConfig)new MsoyGameParser().parseGame(game).match;
            }
            if (match == null) {
                log.warning("Game missing match configuration", "game", game);
            }
        } catch (Exception e) {
            log.warning("Failed to parse XML game definition", "game", game, e);
        }
        if (match != null) {
            return new int[] {
                match.minSeats,
                (match.getMatchType() == GameConfig.PARTY) ? Integer.MAX_VALUE : match.maxSeats
            };
        }
        return new int[] { 1, 2 }; // arbitrary defaults
    }

    /**
     * Returns true if the supplied id references a developer's in-progress original game rather
     * than one listed in the catalog.
     */
    public static boolean isDevelopmentVersion (int gameId)
    {
        return (gameId < 0);
    }
}
