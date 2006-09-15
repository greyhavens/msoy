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

    /** A hash code identifying the game media. */
    public byte[] gameMediaHash;

    /** The MIME type of the {@link #gameMediaHash} media. */
    public byte gameMimeType;

    /**
     * Returns a media descriptor for the actual game media.
     */
    public MediaDesc getGameMedia ()
    {
        return new MediaDesc(gameMediaHash, gameMimeType);
    }

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
        return (gameMediaHash != null);
    }
}
