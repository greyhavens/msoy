//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** The minimum number of players. */
    public short minPlayers;

    /** The maximum number of players. */
    public short maxPlayers;

    /** The desired number of players. */
    public short desiredPlayers;

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
        return getFurniMedia(); // TODO: support logos?
    }

    /**
     * Returns a media descriptor for the media to be used
     * as a table background image.
     */
    public MediaDesc getTableMedia ()
    {
        return (tableMedia != null) ? tableMedia :
            new StaticMediaDesc(StaticMediaDesc.TABLE, GAME);
    }

    // @Override
    public boolean isConsistent ()
    {
        if (!super.isConsistent() || !nonBlank(name)) {
            return false;
        }
        if (minPlayers < 1 || minPlayers > maxPlayers ||
            desiredPlayers < minPlayers || desiredPlayers > maxPlayers) {
            return false;
        }
        return (gameMedia != null);
    }
}
