//
// $Id$

package com.threerings.msoy.game.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.game.data.all.Trophy;

/**
 * The asynchronous (client-side) version of {@link GameService}.
 */
public interface GameServiceAsync
{
    /**
     * The asynchronous version of {@link GameService#loadGameDetail}.
     */
    void loadGameDetail (int gameId, AsyncCallback<GameDetail> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameMetrics}.
     */
    void loadGameMetrics (int gameId, AsyncCallback<GameMetrics> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameLogs}.
     */
    void loadGameLogs (int gameId, AsyncCallback<GameLogs> callback);

    /**
     * The asynchronous version of {@link GameService#updateGameInstructions}.
     */
    public void updateGameInstructions (int gameId, String instructions,
                                        AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#resetGameScores}.
     */
    public void resetGameScores (int gameId, boolean single, int gameMode,
                                 AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameTrophies}.
     */
    public void loadGameTrophies (int gameId, AsyncCallback<List<Trophy>> callback);

    /**
     * The asynchronous version of {@link GameService#compareTrophies}.
     */
    public void compareTrophies (int gameId, int[] memberIds,
                                 AsyncCallback<GameService.CompareResult> callback);

    /**
     * The asynchronous version of {@link GameService#loadTrophyCase}.
     */
    void loadTrophyCase (int memberId, AsyncCallback<TrophyCase> callback);

    /**
     * The asynchronous version of {@link GameService#loadTopRanked}.
     */
    public void loadTopRanked (int gameId, boolean onlyMyFriends,
                               AsyncCallback<PlayerRating[][]> callback);

    /**
     * The asynchronous version of {@link GameService#loadArcadeData}.
     */
    void loadArcadeData (AsyncCallback<ArcadeData> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameGenre}.
     */
    public void loadGameGenre (byte genre, byte sortMethod, String query,
                               AsyncCallback<List<GameInfo>> callback);

    /**
     * The asynchronous version of {@link GameService#loadTopGamesData}.
     */
    void loadTopGamesData (AsyncCallback<FeaturedGameInfo[]> callback);
}
