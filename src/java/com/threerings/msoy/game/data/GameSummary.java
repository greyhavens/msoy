//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameInfo;

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

    /** The thumbnail media for the game we're summarizing. */
    public MediaDesc thumbMedia;

    /** Used for unserialization. */
    public GameSummary ()
    {
    }

    /**
     * Creates a summary for the specified game.
     */
    public GameSummary (GameInfo game)
    {
        this(game.gameId, game.name, game.isAVRG, game.thumbMedia);
    }

    /**
     * Creates a summary for the specified game.
     */
    public GameSummary (int gameId, String name, boolean isAVRG, MediaDesc thumbMedia)
    {
        this.gameId = gameId;
        this.name = name;
        this.avrGame = isAVRG;
        this.thumbMedia = thumbMedia;
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
