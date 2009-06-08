//
// $Id$

package com.threerings.msoy.game.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Provides game related services.
 */
public interface GameService extends RemoteService
{
    /** Return result for {@link #compareTrophies}. */
    public static class CompareResult implements IsSerializable
    {
        /** The name of the game for which we're comparing trophies. */
        public String gameName;

        /** The thumbnail icon for the game in question. */
        public MediaDesc gameThumb;

        /** The trophies for the game in question, in display order. */
        public Trophy[] trophies;

        /** The members being compared. */
        public MemberCard[] members;

        /** When the members earned the trophies. Array elements may be null if the a member has
         * not earned a particular trophy. */
        public Long[][] whenEarneds;
    }

    /** Return result for {@link #loadGameData}. */
    public static class GameData implements IsSerializable
    {
        public GameInfo info;
        public FacebookInfo facebook;
        public GameCode devCode;
        public GameCode pubCode;
    }

    /**
     * Return result for {@link #loadTopGames}.
     */
    public static class TopGamesResult
        implements IsSerializable
    {
        /** The info for the top games, in order. */
        public GameInfo[] topGames;

        /** The ids of the top games games that are also featured. */
        public int[] featured;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/gamesvc";

    /**
     * Loads up information for the arcade.
     */
    ArcadeData loadArcadeData ()
        throws ServiceException;

    /**
     * Loads up information for the main facebook games page.
     */
    ArcadeData loadFBArcadeData ()
        throws ServiceException;

    /**
     * Loads up information on the specified game genre.
     */
    List<GameInfo> loadGameGenre (GameGenre genre, String query)
        throws ServiceException;

    /**
     * Loads up the games created by the caller.
     */
    List<GameInfo> loadMyGames ()
        throws ServiceException;

    /**
     * Loads the details for the specified game.
     */
    GameDetail loadGameDetail (int gameId)
        throws ServiceException;

    /**
     * Loads the metrics for the specified game. Caller must be an admin or the owner of the game.
     */
    GameDistribs loadGameMetrics (int gameId)
        throws ServiceException;

    /**
     * Loads the logs for the specified game. Caller must be an admin or the owner of the game.
     */
    GameLogs loadGameLogs (int gameId)
        throws ServiceException;

    /**
     * Requests to reset the percentiler scores for the game in question. Caller must be an admin
     * or the owner of the game.
     *
     * @param single if true the single player scores will be reset, if false the multiplayer
     * scores will be reset.
     */
    void resetGameScores (int gameId, boolean single, int gameMode)
        throws ServiceException;

    /**
     * Loads and returns the trophies awarded by the specified game. Filling in when they were
     * earned by the caller if possible.
     */
    List<Trophy> loadGameTrophies (int gameId)
        throws ServiceException;

    /**
     * Compares the trophy earnings for the specified set of members in the specified game.
     */
    CompareResult compareTrophies (int gameId, int[] memberIds)
        throws ServiceException;

    /**
     * Loads all trophies owned by the specified member.
     */
    TrophyCase loadTrophyCase (int memberId)
        throws ServiceException;

    /**
     * Returns the top-ranked players for the specified game.
     *
     * @param onlyMyFriends if true, only the player and their friends will be included in the
     * rankings.
     *
     * @return two arrays, the first the single-player rankings for this game, the second its
     * multiplayer rankings.
     */
    PlayerRating[][] loadTopRanked (int gameId, boolean onlyMyFriends)
        throws ServiceException;

    /**
     * Awards a game a rating from 1 to 5.
     */
    RatingResult rateGame (int gameId, byte rating)
        throws ServiceException;

    /**
     * Loads the metadata for the specified game (used by the game editor). Caller must be the game
     * owner or support+.
     */
    GameData loadGameData (int gameId)
        throws ServiceException;

    /**
     * Loads a game's original subitems (level pack, prize, etc.). Caller must be the game owner or
     * support+.
     */
    List<Item> loadGameItems (int gameId, byte type)
        throws ServiceException;

    /**
     * Creates a new game with the supplied basic configuration.
     *
     * @return the id of the newly created game.
     */
    int createGame (boolean isAVRG, String name, MediaDesc thumbMedia, MediaDesc clientCode)
        throws ServiceException;

    /**
     * Updates the metadata for the supplied game.
     */
    void updateGameInfo (GameInfo info)
        throws ServiceException;

    /**
     * Updates the instructions for the specified game. The caller must be the owner of the game's
     * source item.
     */
    void updateGameInstructions (int gameId, String instructions)
        throws ServiceException;

    /**
     * Updates the development code for the supplied game.
     */
    void updateGameCode (GameCode code)
        throws ServiceException;

    /**
     * Publishes the specified game's development code.
     */
    void publishGameCode (int gameId)
        throws ServiceException;

    /**
     * Updates the Facebook info for the supplied game.
     */
    void updateFacebookInfo (FacebookInfo info)
        throws ServiceException;

    /**
     * Loads the games that are used to generate the given top game page.
     */
    TopGamesResult loadTopGames (ArcadeData.Page page)
        throws ServiceException;

    /**
     * Loads the games that are used to generate the given top game page.
     */
    int[] loadTopGameIds (ArcadeData.Page page)
        throws ServiceException;

    /**
     * Removes a top game from the given top game page.
     */
    void removeTopGame (ArcadeData.Page page, int gameId)
        throws ServiceException;

    /**
     * Adds a top game to the given top game page (at the end of the list).
     */
    void addTopGame (ArcadeData.Page page, int gameId)
        throws ServiceException;

    /**
     * Updates top game information for a given arcade page.
     */
    void updateTopGames (ArcadeData.Page page, List<Integer> topGames, Set<Integer> featured,
        Set<Integer> removed)
        throws ServiceException;
}
