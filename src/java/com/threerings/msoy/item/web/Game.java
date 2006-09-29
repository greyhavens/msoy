//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** The name of the game. */
    public String name;

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

    // @Override from Item
    public String getType ()
    {
        return "GAME";
    }

    // @Override from Item
    public String getDescription ()
    {
        return name;
    }

    // @Override
    public boolean isConsistent ()
    {
        if (!super.isConsistent()) {
            return false;
        }
        if (minPlayers < 2 || minPlayers > maxPlayers ||
                desiredPlayers < minPlayers ||
                desiredPlayers > maxPlayers) {
            return false;
        }
        return (gameMedia != null);
    }
}
