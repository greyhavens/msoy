//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains metadata about a game for which a player is currently matchmaking.
 */
public class GameSummary extends SimpleStreamableObject
    implements Cloneable
{
    /** The game id. */
    public int gameId;

    /** True if this summary is for the development version of the game. */
    public boolean isDevelopment;

    /** The name of the game - used as a tooltip */
    public String name;

    /** The description of the game - used for sharing */
    public String description;

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
    public GameSummary (int gameId, boolean isDevelopment, String name, String description,
                        boolean isAVRG, MediaDesc thumbMedia)
    {
        this.gameId = gameId;
        this.isDevelopment = isDevelopment;
        this.name = name;
        this.description = description;
        this.avrGame = isAVRG;
        this.thumbMedia = thumbMedia;
    }

    /**
     * Returns -gameId if we're the development version of the game and +gameId if we're the
     * published version. This typed id is used in various places where we have to encode whether
     * or not we're a development version of the game in the identifier.
     */
    public int getTypedId ()
    {
        return isDevelopment ? -gameId : gameId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof GameSummary) {
            GameSummary data = (GameSummary) other;
            return data.gameId == this.gameId && data.isDevelopment == this.isDevelopment;
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
