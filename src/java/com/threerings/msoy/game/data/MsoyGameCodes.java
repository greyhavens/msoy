//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.parlor.game.data.GameCodes;

/**
 * Codes and constants relating to the MSOY game services.
 */
public interface MsoyGameCodes extends GameCodes
{
    /** A message sent (on the PlayerObject) when the player earns a trophy. The payload will be a
     * Trophy object. */
    public static final String TROPHY_AWARDED = "TrophyAwarded";

    /** A message sent (on the PlayerObject) when the player earns a prize. The payload will be an
     * Item object. */
    public static final String PRIZE_AWARDED = "PrizeAwarded";

    /** An error code reported when a game tries to award an unknown trophy. */
    public static final String E_UNKNOWN_TROPHY = "e.unknown_trophy";

    /** An error code reported when a game tries to award an unknown prize. */
    public static final String E_UNKNOWN_PRIZE = "e.unknown_prize";

    /** Reports to prize awarders that they're doing things wrong. */
    public static final String E_PRIZE_CREATOR_MISMATCH = "e.prize_creator_mismatch";

    /** An error code reported when a game is not found. */
    public static final String E_NO_SUCH_GAME = "e.no_such_game";
    
    /** An error code reported when a game's content could not be parsed or has internal errors. */ 
    public static final String E_BAD_GAME_CONTENT = "e.bad_game_content";
}
