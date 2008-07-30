//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

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

    /** The game's thumbnail media. */
    public MediaDesc gameThumb;

    /** The single player rating of the user for this game. */
    public int singleRating;

    /** The multiplayer rating of the user for this game. */
    public int multiRating;

    /** Used when unserializing */
    public GameRating ()
    {
    }

    @Override // from Object
    public int hashCode ()
    {
        return gameId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof GameRating) && gameId == ((GameRating)other).gameId;
    }
}
