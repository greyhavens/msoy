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
}
