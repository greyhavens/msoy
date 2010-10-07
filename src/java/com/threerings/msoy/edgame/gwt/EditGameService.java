//
// $Id$

package com.threerings.msoy.edgame.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.facebook.gwt.FacebookInfo;
import com.threerings.msoy.facebook.gwt.FeedThumbnail;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.MochiGameInfo;

import com.threerings.msoy.item.data.all.GameItem;

import com.threerings.msoy.item.data.all.MsoyItemType;

/**
 * Provides game editing related services.
 */
@RemoteServiceRelativePath(EditGameService.REL_PATH)
public interface EditGameService extends RemoteService
{
    /** Return result for {@link #loadGameData}. */
    public static class GameData implements IsSerializable
    {
        public GameInfo info;
        public boolean blingPool; // not in GameInfo- it's private!
        public FacebookInfo facebook;
        public GameCode devCode;
        public GameCode pubCode;
    }

    /**
     * Return result for {@link #loadArcadeEntries}.
     */
    public static class ArcadeEntriesResult
        implements IsSerializable
    {
        /** The info for the arcade entries, in order. */
        public List<GameInfo> entries;

        /** The ids of the entries that are also featured. */
        public List<Integer> featured;
    }

    /**
     * Result element for {@link #loadGameItems}.
     */
    public static class GameItemEditorInfo
        implements IsSerializable
    {
        /** The item. */
        public GameItem item;

        /** Whether the item's listing is out of date. */
        public boolean listingOutOfDate;
    }

    /**
     * A mochi game bucket, one for each slot in the Facebook featured games panel, 1-5.
     */
    public static class MochiGameBucket
        implements IsSerializable
    {
        /** The games in the bucket. */
        public List<MochiGameInfo> games;

        /** The index of the currently active game. */
        public int current;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/edgamesvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + EditGameService.ENTRY_POINT;

    /**
     * Loads up the games created by the caller.
     */
    List<GameInfo> loadMyGames ()
        throws ServiceException;

    /**
     * Loads the metadata for the specified game (used by the game editor). Caller must be the game
     * owner or support+.
     */
    GameData loadGameData (int gameId)
        throws ServiceException;

    /**
     * Loads all feed thumbnails for the specified game.
     */
    List<FeedThumbnail> loadFeedThumbnails (int gameId)
        throws ServiceException;

    /**
     * Loads a game's original subitems (level pack, prize, etc.). Caller must be the game owner or
     * support+.
     */
    List<GameItemEditorInfo> loadGameItems (int gameId, MsoyItemType type)
        throws ServiceException;

    /**
     * Creates a new game with the supplied basic configuration.
     *
     * @return the id of the newly created game.
     */
    int createGame (boolean isAVRG, String name, MediaDesc thumbMedia, MediaDesc clientCode)
        throws ServiceException;

    /**
     * Requests that the specified game be deleted. The caller must be the owner and the game must
     * meet various criteria to allow it to be deleted. See the implementation for details.
     */
    void deleteGame (int gameId)
        throws ServiceException;

    /**
     * Updates the metadata for the supplied game.
     */
    void updateGameInfo (GameInfo info, boolean blingPool)
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
     * Updates the feed thumbnails for the specified game. All prior thumbnails are overwritten.
     */
    void updateFeedThumbnails (int gameId, List<FeedThumbnail> thumbnails)
        throws ServiceException;

    /**
     * Loads the arcade entries that are shown in the given portal. Support only.
     */
    ArcadeEntriesResult loadArcadeEntries (ArcadeData.Portal portal)
        throws ServiceException;

    /**
     * Loads the ids of the arcade entries that are shown in the given portal. Support only.
     */
    int[] loadArcadeEntryIds (ArcadeData.Portal portal)
        throws ServiceException;

    /**
     * Removes an arcade entry from the given portal. Support only.
     */
    void removeArcadeEntry (ArcadeData.Portal portal, int gameId)
        throws ServiceException;

    /**
     * Adds an arcade entry to the given portal (at the end of the list). Support only.
     */
    void addArcadeEntry (ArcadeData.Portal portal, int gameId)
        throws ServiceException;

    /**
     * Updates arcade entry information for a given portal. Support only.
     */
    void updateArcadeEntries (ArcadeData.Portal portal, List<Integer> entries,
        Set<Integer> featured, Set<Integer> removed)
        throws ServiceException;

    /**
     * Get the info on all mochi games for the specified bucket (1-5).
     */
    MochiGameBucket getMochiBucket (int bucket)
        throws ServiceException;

    /**
     * Sets the tags for the given mochi bucket. Imports the mochi game info for all tags that are
     * not yet imported. Returns a list of tags that could not be imported, if any. Throws an
     * exception if none of the tags could be imported.
     */
    List<String> setMochiBucketTags (int slot, String[] ids)
        throws ServiceException;

    /**
     * Tests the advancement of the current featured mochi games.
     */
    void testRollFeaturedGames ()
        throws ServiceException;
}
