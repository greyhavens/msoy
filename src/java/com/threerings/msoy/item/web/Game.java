//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Extends MediaItem with game info.
 */
public class Game extends MediaItem
{
    /** The name of the game. */
    public String name;

    /** The minimum number of players. */
    public short minPlayers;

    /** The maximum number of players. */
    public short maxPlayers;

    /** The desired number of players. */
    public short desiredPlayers;

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

    // @Override from Item
    public String getThumbnailPath ()
    {
        // The games aren't really standalone, so we can't show
        // the game as the thumbnail...
        return "/media/static/game.png"; // TODO?
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
        return true;
    }
}
