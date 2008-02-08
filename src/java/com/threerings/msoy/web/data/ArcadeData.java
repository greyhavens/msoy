//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information displayed in the arcade.
 */
public class ArcadeData
    implements IsSerializable
{
    /** Contains summary information on a particular game genre. */
    public static class Genre
        implements IsSerializable
    {
        /** The number of games we highlight in each genre. */
        public static final int HIGHLIGHTED_GAMES = 2;

        /** This genre's code. */
        public byte genre;

        /** The total number of games in this genre. */
        public int gameCount;

        /** The highlighted games in this genre. */
        public GameInfo[] games;
    }

    /** Info on the featured game. */
    public FeaturedGameInfo featuredGame;

    /** 
     * Information about each game genre.
     * 
     * @gwt.typeArgs <com.threerings.msoy.web.data.ArcadeData.Genre>
     */
    public List genres;

    /** 
     * Info for this player's favorite games or null if they're a guest or have none.
     * 
     * @gwt.typeArgs <com.threerings.msoy.web.data.GameInfo>
     */
    public List favorites;
}
