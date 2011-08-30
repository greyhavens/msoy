//
// $Id$

package com.threerings.msoy.game.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.game.data.all.Trophy;

/**
 * Provides the asynchronous version of {@link GameService}.
 */
public interface GameServiceAsync
{
    /**
     * The async version of {@link GameService#loadGameLogs}.
     */
    void loadGameLogs (int gameId, AsyncCallback<GameLogs> callback);

    /**
     * The async version of {@link GameService#loadArcadeData}.
     */
    void loadArcadeData (ArcadeData.Portal portal, AsyncCallback<ArcadeData> callback);

    /**
     * The async version of {@link GameService#loadGameGenre}.
     */
    void loadGameGenre (ArcadeData.Portal portal, GameGenre genre, String query, AsyncCallback<List<GameInfo>> callback);

    /**
     * The async version of {@link GameService#loadGameDetail}.
     */
    void loadGameDetail (int gameId, AsyncCallback<GameDetail> callback);

    /**
     * The async version of {@link GameService#loadGameMetrics}.
     */
    void loadGameMetrics (int gameId, AsyncCallback<GameDistribs> callback);

    /**
     * The async version of {@link GameService#resetGameScores}.
     */
    void resetGameScores (int gameId, boolean single, int gameMode, AsyncCallback<Void> callback);

    /**
     * The async version of {@link GameService#loadGameTrophies}.
     */
    void loadGameTrophies (int gameId, AsyncCallback<List<Trophy>> callback);

    /**
     * The async version of {@link GameService#compareTrophies}.
     */
    void compareTrophies (int gameId, int[] memberIds, AsyncCallback<GameService.CompareResult> callback);

    /**
     * The async version of {@link GameService#loadTrophyCase}.
     */
    void loadTrophyCase (int memberId, AsyncCallback<TrophyCase> callback);

    /**
     * The async version of {@link GameService#loadTopRanked}.
     */
    void loadTopRanked (int gameId, boolean onlyMyFriends, AsyncCallback<PlayerRating[][]> callback);

    /**
     * The async version of {@link GameService#rateGame}.
     */
    void rateGame (int gameId, byte rating, AsyncCallback<RatingResult> callback);

    /**
     * The async version of {@link GameService#updateGameInstructions}.
     */
    void updateGameInstructions (int gameId, String instructions, AsyncCallback<Void> callback);

    /**
     * The async version of {@link GameService#getMochiGame}.
     */
    void getMochiGame (String mochiTag, AsyncCallback<MochiGameInfo> callback);
}
