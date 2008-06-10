//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link GameService}.
 */
public interface GameServiceAsync
{
    /**
     * The asynchronous version of {@link GameService#loadGameDetail}.
     */
    public void loadGameDetail (WebIdent ident, int gameId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#loadGameMetrics}.
     */
    public void loadGameMetrics (WebIdent ident, int gameId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#loadGameLogs}.
     */
    public void loadGameLogs (WebIdent ident, int gameId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#updateGameInstructions}.
     */
    public void updateGameInstructions (WebIdent ident, int gameId, String instructions,
                                        AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#resetGameScores}.
     */
    public void resetGameScores (WebIdent ident, int gameId, boolean single,
                                 AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#loadGameTrophies}.
     */
    public void loadGameTrophies (WebIdent ident, int gameId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#compareTrophies}.
     */
    public void compareTrophies (WebIdent ident, int gameId, int[] memberIds,
                                 AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#loadTrophyCase}.
     */
    public void loadTrophyCase (WebIdent ident, int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#loadTopRanked}.
     */
    public void loadTopRanked (WebIdent ident, int gameId, boolean onlyMyFriends,
                               AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#loadArcadeData}.
     */
    public void loadArcadeData (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#loadGameGenre}.
     */
    public void loadGameGenre (WebIdent ident, byte genre, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#loadTopGamesData}.
     */
    public void loadTopGamesData (WebIdent ident, AsyncCallback callback);
}
