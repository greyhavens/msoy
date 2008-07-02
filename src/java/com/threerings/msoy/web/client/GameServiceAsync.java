//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.ArcadeData;
import com.threerings.msoy.web.data.FeaturedGameInfo;
import com.threerings.msoy.web.data.GameDetail;
import com.threerings.msoy.web.data.GameInfo;
import com.threerings.msoy.web.data.GameLogs;
import com.threerings.msoy.web.data.GameMetrics;
import com.threerings.msoy.web.data.PlayerRating;
import com.threerings.msoy.web.data.TrophyCase;
import com.threerings.msoy.web.data.WebIdent;

import com.threerings.msoy.game.data.all.Trophy;

/**
 * The asynchronous (client-side) version of {@link GameService}.
 */
public interface GameServiceAsync
{
    /**
     * The asynchronous version of {@link GameService#loadGameDetail}.
     */
    public void loadGameDetail (WebIdent ident, int gameId, AsyncCallback<GameDetail> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameMetrics}.
     */
    public void loadGameMetrics (WebIdent ident, int gameId, AsyncCallback<GameMetrics> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameLogs}.
     */
    public void loadGameLogs (WebIdent ident, int gameId, AsyncCallback<GameLogs> callback);

    /**
     * The asynchronous version of {@link GameService#updateGameInstructions}.
     */
    public void updateGameInstructions (WebIdent ident, int gameId, String instructions,
                                        AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#resetGameScores}.
     */
    public void resetGameScores (WebIdent ident, int gameId, boolean single, 
                                 AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameTrophies}.
     */
    public void loadGameTrophies (WebIdent ident, int gameId, 
                                  AsyncCallback<List<Trophy>> callback);

    /**
     * The asynchronous version of {@link GameService#compareTrophies}.
     */
    public void compareTrophies (WebIdent ident, int gameId, int[] memberIds,
                                 AsyncCallback<GameService.CompareResult> callback);

    /**
     * The asynchronous version of {@link GameService#loadTrophyCase}.
     */
    public void loadTrophyCase (WebIdent ident, int memberId, AsyncCallback<TrophyCase> callback);

    /**
     * The asynchronous version of {@link GameService#loadTopRanked}.
     */
    public void loadTopRanked (WebIdent ident, int gameId, boolean onlyMyFriends, 
                               AsyncCallback<PlayerRating[][]> callback);

    /**
     * The asynchronous version of {@link GameService#loadArcadeData}.
     */
    public void loadArcadeData (WebIdent ident, AsyncCallback<ArcadeData> callback);

    /**
     * The asynchronous version of {@link GameService#loadGameGenre}.
     */
    public void loadGameGenre (WebIdent ident, byte genre, byte sortMethod, String query, 
                               AsyncCallback<List<GameInfo>> callback);

    /**
     * The asynchronous version of {@link GameService#loadTopGamesData}.
     */
    public void loadTopGamesData (WebIdent ident, AsyncCallback<FeaturedGameInfo[]> callback);
}
