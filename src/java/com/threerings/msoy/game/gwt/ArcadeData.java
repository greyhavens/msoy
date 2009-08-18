//
// $Id$

package com.threerings.msoy.game.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.util.ByteEnum;

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
        public GameGenre genre;

        /** The total number of games in this genre. */
        public int gameCount;

        /** The highlighted games in this genre. */
        public GameCard[] games;
    }

    /** Enumerates the various display modes and data sets for the arcade page (#games). */
    public enum Portal
        implements ByteEnum, IsSerializable
    {
        MAIN(0),
        FACEBOOK(1) {
            @Override public boolean showGenre (GameGenre genre)
            {
                return genre != GameGenre.MMO_WHIRLED;
            }

            @Override public boolean isFiltered ()
            {
                return true;
            }
        };

        /**
         * Tests whether the given genre should be displayed when viewing a list of genres.
         */
        public boolean showGenre (GameGenre genre)
        {
            return true;
        }

        /**
         * Tests whether the arcade's content is filtered.
         */
        public boolean isFiltered ()
        {
            return false;
        }

        @Override // from ByteEnum
        public byte toByte ()
        {
            return _value;
        }

        Portal (int value)
        {
            _value = (byte)value;
        }

        protected byte _value;
    }

    /** The number of featured games we show on the Arcade page. */
    public static int FEATURED_GAME_COUNT = 5;

    /** TEMP. Will be populated instead of featuredGames for the FB arcade. */
    public MochiGameInfo[] mochiGames;

    /** Info on the featured games. */
    public GameInfo[] featuredGames;

    /**
     * Information about each game genre. Not used in Facebook portal mode.
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

    /**
     * More games that are not in the top games. First few will be a random selection of highly
     * ranked games. Following is a random selection of all games. Only used in Facebook portal
     * mode.
     */
    public List<GameCard> gameWall;
}
