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

    /** We reserve a very unlikely gameId for the tutorial. */
    public static final int TUTORIAL_GAME_ID = Integer.MAX_VALUE;

    /** The XML game configuration. */
    public String config;

    /** The primary game media. */
    public MediaDesc gameMedia;

    /** A unique identifier assigned to this game and preserved across new versions of the game
     * item so that ratings and lobbies and content packs all reference the same "game". */
    public int gameId;

    /**
     * Returns true if this is a developer's in-progress original game rather than one listed in
     * the catalog.
     */
    public boolean isDeveloperVersion ()
    {
        return (gameId < 0);
    }

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
        return (isInWorld() ?
                new byte[] { LEVEL_PACK, ITEM_PACK, TROPHY_SOURCE, PRIZE, MOB, PROP } :
                new byte[] { LEVEL_PACK, ITEM_PACK, TROPHY_SOURCE, PRIZE, });
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return (furniMedia != null) ? furniMedia : getThumbnailMedia();
    }

    /**
     * Checks whether this game is an in-world, as opposed to lobbied, game.
     */
    public boolean isInWorld ()
    {
        return (config != null) && (config.indexOf("<avrg/>") >= 0);
    }

    // @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (gameMedia != null);
    }
}
