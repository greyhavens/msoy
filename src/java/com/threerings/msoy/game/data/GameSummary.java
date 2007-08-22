//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains metadata about a game for which a player is currently matchmaking.
 */
public class GameSummary extends SimpleStreamableObject
    implements Cloneable
{
    /** The game id */
    public int gameId;

    /** The name of the game - used as a tooltip */
    public String name;

    /** The mime type of this game's client media (SWF or JAR). */
    public byte gameMediaType;

    /** The thumbnail of the game - used as a game icon */
    public MediaDesc thumbMedia;

    /** Used for unserialization. */
    public GameSummary ()
    {
    }

    /**
     * Creates a summary for the specified game.
     */
    public GameSummary (Game game)
    {
        gameId = game.gameId;
        name = game.name;
        gameMediaType = game.gameMedia.mimeType;
        thumbMedia = game.thumbMedia;
    }

    /**
     * Returns the thumbnail media for the game we're summarizing.
     */
    public MediaDesc getThumbMedia ()
    {
        return thumbMedia != null ? thumbMedia : Item.getDefaultThumbnailMediaFor(Item.GAME);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof GameSummary) {
            GameSummary data = (GameSummary) other;
            return data.gameId == this.gameId;
        }
        return false;
    }

    @Override // from Object
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse); // not going to happen
        }
    }
}
