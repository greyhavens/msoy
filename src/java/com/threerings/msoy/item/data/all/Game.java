//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** Identifies our lobby background table media. */
    public static final String TABLE_MEDIA = "table";

    /** Defines the number of different game types. See GameConfig. */
    public static final int GAME_TYPES = 3;

    /** The maximum length of game identifiers (used by level and item packs and trophies). */
    public static final int MAX_IDENT_LENGTH = 32;

    /** The XML game configuration. */
    public String config;

    /** The primary game media. */
    public MediaDesc gameMedia;

    /** A unique identifier assigned to this game and preserved across new versions of the game
     * item so that ratings and lobbies and content packs all reference the same "game". */
    public int gameId;

    // @Override from Item
    public byte getType ()
    {
        return GAME;
    }

    // @Override from Item
    public byte[] getSalableSubTypes ()
    {
        return new byte[] {
            LEVEL_PACK, ITEM_PACK,
        };
    }

    // @Override from Item
    public byte[] getSubTypes ()
    {
        return new byte[] {
            LEVEL_PACK, ITEM_PACK, TROPHY_SOURCE, /* PRIZE */
        };
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getThumbnailMedia(); // TODO: support logos?
    }

    /**
     * Checks whether this game is an in-world, as opposed to lobbied, game.
     */
    public boolean isInWorld ()
    {
        // TODO: this will change
        return (0 <= config.indexOf("<toggle ident=\"avrg\" start=\"true\"/>")) ||
            (0 <= config.indexOf("<toggle ident=\"chiyogami\" start=\"true\"/>"));
    }

    // @Override
    public boolean isConsistent ()
    {
        // TODO: Check over the values in the XML to make sure they are sane
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (gameMedia != null);
    }
}
