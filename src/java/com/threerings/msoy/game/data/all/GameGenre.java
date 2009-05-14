//
// $Id$

package com.threerings.msoy.game.data.all;

/**
 * Enumerates our game genres.
 */
public class GameGenre
{
    /** Used when loading games by genre. */
    public static final byte ALL = Byte.MIN_VALUE;

    /** A genre constant. */
    public static final byte HIDDEN = -1;

    /** A genre constant. */
    public static final byte OTHER = 0;

    /** A genre constant. */
    public static final byte WORD = 1;

    /** A genre constant. */
    public static final byte CARD_BOARD = 2;

    /** A genre constant. */
    public static final byte PUZZLE = 3;

    /** A genre constant. */
    public static final byte STRATEGY = 4;

    /** A genre constant. */
    public static final byte ACTION_ARCADE = 5;

    /** A genre constant. */
    public static final byte ADVENTURE_RPG = 6;

    /** A genre constant. */
    public static final byte SPORTS_RACING = 7;

    /** A genre constant. */
    public static final byte MMO_WHIRLED = 8;

    /** All game genres, in display order. */
    public static final byte[] GENRES = { ACTION_ARCADE, MMO_WHIRLED, STRATEGY, PUZZLE,
                                          ADVENTURE_RPG, WORD, CARD_BOARD, SPORTS_RACING, OTHER };
}
