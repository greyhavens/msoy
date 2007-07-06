//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information about an an implicit user's rating for a (also included) game.
 */
public class GameRating
    implements IsSerializable
{
    /** The game's id. */
    public int gameId;
    
    /** The game's name. */
    public String gameName;
    
    /** The actual rating of the implicit user, for this game. This value is scaled to [0, 1]. */
    public float rating;

    /** Used when unserializing */
    public GameRating ()
    {
    }

    /**
     * Creates a {@link GameRating} instance populated with the given data.
     */
    public GameRating (int gameId, String gameName, float rating)
    {
        this.gameId = gameId;
        this.gameName = gameName;
        this.rating = rating;
    }

    // @Override // from Object
    public int hashCode ()
    {
        return gameId;
    }

    // @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof GameRating) && gameId == ((GameRating)other).gameId;
    }
}
