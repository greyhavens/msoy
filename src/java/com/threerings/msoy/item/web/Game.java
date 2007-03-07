//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** Identifies our lobby background table media. */
    public static final String TABLE_MEDIA = "table";

    /** Defines the number of different game types. See GameConfig. */
    public static final int GAME_TYPES = 3;

    /** The type of game, see GameConfig. */
    public byte gameType;

    /** The minimum number of players. */
    public short minPlayers;

    /** The maximum number of players. */
    public short maxPlayers;

    /** Is this game unwatchable? Applicable only for non-continuous games. */
    public boolean unwatchable;

    /** The XML game configuration. */
    public String config;

    /** The primary game media. */
    public MediaDesc gameMedia;

    /** The game's table background. */
    public MediaDesc tableMedia;

    // @Override from Item
    public byte getType ()
    {
        return GAME;
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
        // TODO: parse configuration as XML
        return config.trim().equals("avrg") ||
            config.startsWith("Chiyogami");
    }

    /**
     * Returns a media descriptor for the media to be used
     * as a table background image.
     */
    public MediaDesc getTableMedia ()
    {
        return (tableMedia != null) ? tableMedia :
            new StaticMediaDesc(MediaDesc.IMAGE_PNG, GAME, TABLE_MEDIA);
    }

    // @Override
    public boolean isConsistent ()
    {
        if (!super.isConsistent() || !nonBlank(name)) {
            return false;
        }
        if (minPlayers < 1 || minPlayers > maxPlayers) {
            return false;
        }
        return (gameMedia != null);
    }
}
