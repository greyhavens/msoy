//
// $Id$

package com.threerings.msoy.edgame.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.facebook.gwt.FacebookInfo;
import com.threerings.msoy.facebook.gwt.FeedThumbnail;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.item.data.all.MsoyItemType;

/**
 * Provides the asynchronous version of {@link EditGameService}.
 */
public interface EditGameServiceAsync
{
    /**
     * The async version of {@link EditGameService#loadMyGames}.
     */
    void loadMyGames (AsyncCallback<List<GameInfo>> callback);

    /**
     * The async version of {@link EditGameService#loadGameData}.
     */
    void loadGameData (int gameId, AsyncCallback<EditGameService.GameData> callback);

    /**
     * The async version of {@link EditGameService#loadFeedThumbnails}.
     */
    void loadFeedThumbnails (int gameId, AsyncCallback<List<FeedThumbnail>> callback);

    /**
     * The async version of {@link EditGameService#loadGameItems}.
     */
    void loadGameItems (int gameId, MsoyItemType type, AsyncCallback<List<EditGameService.GameItemEditorInfo>> callback);

    /**
     * The async version of {@link EditGameService#createGame}.
     */
    void createGame (boolean isAVRG, String name, MediaDesc thumbMedia, MediaDesc clientCode, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link EditGameService#deleteGame}.
     */
    void deleteGame (int gameId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditGameService#updateGameInfo}.
     */
    void updateGameInfo (GameInfo info, boolean blingPool, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditGameService#updateGameCode}.
     */
    void updateGameCode (GameCode code, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditGameService#publishGameCode}.
     */
    void publishGameCode (int gameId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditGameService#updateFeedThumbnails}.
     */
    void updateFeedThumbnails (int gameId, List<FeedThumbnail> thumbnails, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditGameService#loadArcadeEntries}.
     */
    void loadArcadeEntries (ArcadeData.Portal portal, AsyncCallback<EditGameService.ArcadeEntriesResult> callback);

    /**
     * The async version of {@link EditGameService#loadArcadeEntryIds}.
     */
    void loadArcadeEntryIds (ArcadeData.Portal portal, AsyncCallback<int[]> callback);

    /**
     * The async version of {@link EditGameService#removeArcadeEntry}.
     */
    void removeArcadeEntry (ArcadeData.Portal portal, int gameId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditGameService#addArcadeEntry}.
     */
    void addArcadeEntry (ArcadeData.Portal portal, int gameId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditGameService#updateArcadeEntries}.
     */
    void updateArcadeEntries (ArcadeData.Portal portal, List<Integer> entries, Set<Integer> featured, Set<Integer> removed, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditGameService#getMochiBucket}.
     */
    void getMochiBucket (int bucket, AsyncCallback<EditGameService.MochiGameBucket> callback);

    /**
     * The async version of {@link EditGameService#setMochiBucketTags}.
     */
    void setMochiBucketTags (int slot, String[] ids, AsyncCallback<List<String>> callback);

    /**
     * The async version of {@link EditGameService#testRollFeaturedGames}.
     */
    void testRollFeaturedGames (AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditGameService#updateFacebookInfo}.
     */
    void updateFacebookInfo (FacebookInfo info, AsyncCallback<Void> callback);
}
