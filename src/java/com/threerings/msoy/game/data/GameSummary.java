//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.orth.data.MediaDesc;

/**
 * Contains metadata about a game for which a player is currently matchmaking.
 */
@com.threerings.util.ActionScript(omit=true)
public class GameSummary extends SimpleStreamableObject
    implements Cloneable
{
    /** The game id. This will be negative if the summary is for the dev version. */
    public int gameId;

    /** The name of the game - used as a tooltip */
    public String name;

    /** The description of the game - used for sharing */
    public String description;

    /** Whether or not this is an AVRGame. */
    public boolean avrGame;

    /** The thumbnail media for the game we're summarizing. */
    public MediaDesc thumbMedia;

    /** The member id of the creator of the game. */
    public int creatorId;

    /** Used for unserialization. */
    public GameSummary ()
    {
    }

    /**
     * Creates a summary for the specified game.
     */
    public GameSummary (int gameId, String name, String description, boolean isAVRG,
                        MediaDesc thumbMedia, int creatorId)
    {
        this.gameId = gameId;
        this.name = name;
        this.description = description;
        this.avrGame = isAVRG;
        this.thumbMedia = thumbMedia;
        this.creatorId = creatorId;
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
    public int hashCode () {
        return gameId;
    }

    @Override // from Object
    public GameSummary clone ()
    {
        try {
            return (GameSummary) super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse); // not going to happen
        }
    }
}
