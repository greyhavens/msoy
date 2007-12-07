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

    /** A predefined game record for our tutorial game. */
    public static final Game TUTORIAL_GAME = new Game() {
        /* implicit constructor */ {
            this.gameId = TUTORIAL_GAME_ID;
            this.name = "Whirled Tutorial";
            this.config = "<avrg/>";
            this.gameMedia = new StaticMediaDesc(
                MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.GAME, "tutorial");
            // TODO: if we end up using these for AVRG's we'll want hand-crafted stuffs here
            this.thumbMedia = getDefaultThumbnailMediaFor(GAME);
            this.furniMedia = getDefaultFurniMediaFor(GAME);
        }
    };

    /** The width of a game screenshot. */
    public static final int SHOT_WIDTH = 175;

    /** The height of a game screenshot. */
    public static final int SHOT_HEIGHT = 125;

    /** The XML game configuration. */
    public String config;

    /** The primary game media. */
    public MediaDesc gameMedia;

    /** A unique identifier assigned to this game and preserved across new versions of the game
     * item so that ratings and lobbies and content packs all reference the same "game". */
    public int gameId;

    /** The game screenshot media. */
    public MediaDesc shotMedia;

    /**
     * Returns true if the specified game is a developer's in-progress original game rather than
     * one listed in the catalog.
     */
    public static boolean isDeveloperVersion (int gameId)
    {
        return (gameId < 0);
    }

    /**
     * Returns true if this is a developer's in-progress original game rather than one listed in
     * the catalog.
     */
    public boolean isDeveloperVersion ()
    {
        return isDeveloperVersion(gameId);
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
                new byte[] { LEVEL_PACK, ITEM_PACK, TROPHY_SOURCE, PRIZE, PROP } :
                new byte[] { LEVEL_PACK, ITEM_PACK, TROPHY_SOURCE, PRIZE, });
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        if (shotMedia != null) {
            return shotMedia;
        }
        if (furniMedia != null) {
            return furniMedia;
        }
        return getThumbnailMedia();
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
