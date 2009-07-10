//
// $Id$

package com.threerings.msoy.game.gwt;

import com.samskivert.depot.ByteEnum;

/**
 * Enumerates our game genres.
 */
public enum GameGenre
    implements ByteEnum
{
    ALL(-2), HIDDEN(-1), OTHER(0), WORD(1), CARD_BOARD(2), PUZZLE(3), STRATEGY(4),
    ACTION_ARCADE(5), ADVENTURE_RPG(6), SPORTS_RACING(7), MMO_WHIRLED(8);

    /** All game genres, in display order. */
    public static final GameGenre[] DISPLAY_GENRES = {
        ACTION_ARCADE, MMO_WHIRLED, STRATEGY, PUZZLE, ADVENTURE_RPG,
        WORD, CARD_BOARD, SPORTS_RACING, OTHER
    };

    // from interface ByteEnum
    public byte toByte ()
    {
        return _code;
    }

    GameGenre (int code) {
        _code = (byte)code;
    }

    protected byte _code;
}
