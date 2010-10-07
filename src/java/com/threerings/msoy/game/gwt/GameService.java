//
// $Id$

package com.threerings.msoy.game.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.game.data.all.Trophy;

import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Provides game related services.
 */
@RemoteServiceRelativePath(GameService.REL_PATH)
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

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/gamesvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + GameService.ENTRY_POINT;

    /**
     * Loads up information for the arcade.
     */
    ArcadeData loadArcadeData (ArcadeData.Portal portal)
        throws ServiceException;

    /**
     * Loads up information on the specified game genre.
     */
    List<GameInfo> loadGameGenre (ArcadeData.Portal portal, GameGenre genre, String query)
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
     * Updates the instructions for the specified game. The caller must be the creator of the game.
     */
    void updateGameInstructions (int gameId, String instructions)
        throws ServiceException;

    /**
     * Get the info on a mochi game.
     */
    MochiGameInfo getMochiGame (String mochiTag)
        throws ServiceException;
}
