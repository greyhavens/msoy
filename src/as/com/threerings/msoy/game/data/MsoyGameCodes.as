//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.parlor.game.data.GameCodes;

import com.threerings.msoy.data.MsoyCodes;

/**
 * Codes and constants relaying to the MSOY game services.
 */
public class MsoyGameCodes extends GameCodes
{
    /** The message bundle that contains game-related translations. */
    public static const GAME_BUNDLE :String = MsoyCodes.GAME_MSGS;

    /** A message sent when the player earns a trophy. The payload will be a Trophy object. */
    public static const TROPHY_AWARDED :String = "TrophyAwarded";
}
}
