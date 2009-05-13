//
// $Id$

package com.threerings.msoy.game.gwt;

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
        public static final int HIGHLIGHTED_GAMES = 3;

        /** This genre's code. */
        public byte genre;

        /** The total number of games in this genre. */
        public int gameCount;

        /** The highlighted games in this genre. */
        public GameCard[] games;
    }

    /** The number of featured games we show on the Arcade page. */
    public static int FEATURED_GAME_COUNT = 5;

    /** The number of top games to show on the arcade page */
    public static int TOP_GAME_COUNT = 20;

    /** Info on the featured games. */
    public GameInfo[] featuredGames;

    /**
     * Information about each game genre.
     */
    public List<Genre> genres;

    /**
     * List of all games ordered by name.
     */
    public List<GameCard> allGames;

    /**
     * List of top X games ordered by rank.
     */
    public List<GameCard> topGames;
}
