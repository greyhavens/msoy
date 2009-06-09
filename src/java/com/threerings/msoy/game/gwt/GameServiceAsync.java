//
// $Id$

package com.threerings.msoy.game.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Item;

/**
 * The asynchronous (client-side) version of {@link GameService}.
 */
public interface GameServiceAsync
{
    /**
     * The asynchronous version of {@link GameService#loadArcadeData}.
     */
    void loadArcadeData (ArcadeData.Portal portal, AsyncCallback<ArcadeData> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameGenre}.
     */
    void loadGameGenre (GameGenre genre, String query, AsyncCallback<List<GameInfo>> callback);

    /**
     * The asynchronous version of {@link GameService#loadMyGames}.
     */
    void loadMyGames (AsyncCallback<List<GameInfo>> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameDetail}.
     */
    void loadGameDetail (int gameId, AsyncCallback<GameDetail> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameMetrics}.
     */
    void loadGameMetrics (int gameId, AsyncCallback<GameDistribs> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameLogs}.
     */
    void loadGameLogs (int gameId, AsyncCallback<GameLogs> callback);

    /**
     * The asynchronous version of {@link GameService#resetGameScores}.
     */
    void resetGameScores (int gameId, boolean single, int gameMode, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameTrophies}.
     */
    void loadGameTrophies (int gameId, AsyncCallback<List<Trophy>> callback);

    /**
     * The asynchronous version of {@link GameService#compareTrophies}.
     */
    void compareTrophies (int gameId, int[] memberIds,
                          AsyncCallback<GameService.CompareResult> callback);

    /**
     * The asynchronous version of {@link GameService#loadTrophyCase}.
     */
    void loadTrophyCase (int memberId, AsyncCallback<TrophyCase> callback);

    /**
     * The asynchronous version of {@link GameService#loadTopRanked}.
     */
    void loadTopRanked (int gameId, boolean onlyMyFriends,
                        AsyncCallback<PlayerRating[][]> callback);

    /**
     * The asynchronous version of {@link GameService#rateGame}.
     */
    void rateGame (int gameId, byte rating, AsyncCallback<RatingResult> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameData}.
     */
    void loadGameData (int gameId, AsyncCallback<GameService.GameData> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameItems}.
     */
    void loadGameItems (int gameId, byte type, AsyncCallback<List<Item>> callback);

    /**
     * The asynchronous version of {@link GameService#createGame}.
     */
    void createGame (boolean isAVRG, String name, MediaDesc thumbMedia, MediaDesc clientCode,
                     AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link GameService#updateGameInfo}.
     */
    void updateGameInfo (GameInfo info, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#updateGameInstructions}.
     */
    void updateGameInstructions (int gameId, String instructions, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#updateGameCode}.
     */
    void updateGameCode (GameCode code, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#publishGameCode}.
     */
    void publishGameCode (int gameId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#updateFacebookInfo}.
     */
    void updateFacebookInfo (FacebookInfo info, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#loadArcadeEntryIds}.
     */
    void loadArcadeEntryIds (ArcadeData.Portal portal, AsyncCallback<int[]> callback);

    /**
     * The asynchronous version of {@link GameService#loadArcadeEntries}.
     */
    void loadArcadeEntries (ArcadeData.Portal portal,
        AsyncCallback<GameService.ArcadeEntriesResult> callback);

    /**
     * The asynchronous version of {@link GameService#removeArcadeEntry}.
     */
    void removeArcadeEntry (ArcadeData.Portal portal, int gameId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#addArcadeEntry}.
     */
    void addArcadeEntry (ArcadeData.Portal portal, int gameId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#updateArcadeEntries}.
     */
    void updateArcadeEntries (ArcadeData.Portal portal, List<Integer> entries,
        Set<Integer> featured, Set<Integer> removed, AsyncCallback<Void> callback);
}
