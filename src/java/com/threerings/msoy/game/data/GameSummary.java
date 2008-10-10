//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;

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

    /** Whether or not this is an AVRGame. */
    public boolean avrGame;

    /** The mime type of this game's client media (SWF or JAR). */
    public byte gameMediaType;

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
        avrGame = game.isInWorld();
        gameMediaType = game.gameMedia.mimeType;
        _thumbMedia = game.getRawThumbnailMedia();
    }

    /**
     * Returns the thumbnail media for the game we're summarizing.
     */
    public MediaDesc getThumbMedia ()
    {
        return _thumbMedia != null ? _thumbMedia : Item.getDefaultThumbnailMediaFor(Item.GAME);
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

    /** The thumbnail of the game - used as a game icon */
    protected MediaDesc _thumbMedia;
}
