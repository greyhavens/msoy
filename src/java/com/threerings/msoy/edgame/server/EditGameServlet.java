//
// $Id$

package com.threerings.msoy.edgame.server;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

import com.threerings.parlor.rating.server.persist.RatingRepository;

import com.threerings.orth.data.MediaDesc;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.comment.server.persist.CommentRepository;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MediaDescUtil;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.edgame.gwt.EditGameService;
import com.threerings.msoy.edgame.gwt.GameCode;
import com.threerings.msoy.facebook.gwt.FacebookInfo;
import com.threerings.msoy.facebook.gwt.FeedThumbnail;
import com.threerings.msoy.facebook.server.FacebookLogic;
import com.threerings.msoy.facebook.server.persist.FacebookInfoRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.facebook.server.persist.FeedThumbnailRecord;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.MochiGameInfo;
import com.threerings.msoy.game.server.GameNodeActions;
import com.threerings.msoy.game.server.persist.ArcadeEntryRecord;
import com.threerings.msoy.game.server.persist.GameCodeRecord;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.GameItem;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.GameItemRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link GameService}.
 */
public class EditGameServlet extends MsoyServiceServlet
    implements EditGameService
{
    @Override // from interface EditGameService
    public List<GameInfo> loadMyGames ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
        List<GameInfo> infos = Lists.newArrayList();
        for (GameInfoRecord grec : _mgameRepo.loadGamesByCreator(mrec.memberId)) {
            infos.add(grec.toGameInfo(getGamePop(pps, grec.gameId)));
        }
        return infos;
    }

    @Override // from interface EditGameService
    public GameData loadGameData (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        GameInfoRecord info = requireIsGameCreator(gameId, mrec);
        GameData data = new GameData();
        data.info = info.toGameInfo(0);
        data.blingPool = info.blingPool;
        data.facebook = _facebookRepo.loadGameFacebookInfo(info.gameId).toFacebookInfo();
        data.devCode = _mgameRepo.loadGameCode(GameInfo.toDevId(info.gameId), false);
        data.pubCode = _mgameRepo.loadGameCode(info.gameId, false);
        return data;
    }

    @Override // from interface EditGameService
    public List<FeedThumbnail> loadFeedThumbnails (int gameId)
        throws ServiceException
    {
        requireAuthedUser();
        return Lists.newArrayList(Lists.transform(
            _facebookRepo.loadGameThumbnails(gameId), FeedThumbnailRecord.TO_THUMBNAIL));
    }

    @Override // from interface EditGameService
    public void updateFeedThumbnails (final int gameId, List<FeedThumbnail> thumbnails)
        throws ServiceException
    {
        final Set<String> gameOverrideCodes = Sets.newHashSet("trophy", "challenge");
        MemberRecord mrec;
        if (gameId == 0) {
            mrec = requireAdminUser();
        } else {
            mrec = requireAuthedUser();
            requireIsGameCreator(gameId, mrec);
        }
        for (FeedThumbnail thumb : thumbnails) {
            if (thumb.media == null || !thumb.media.isImage()) {
                // this should never happen with a legitimate client
                throw new ServiceException(MsoyCodes.E_INTERNAL_ERROR);
            }
            if (gameId != 0 && !gameOverrideCodes.contains(thumb.code)) {
                // this should never happen with a legitimate client
                throw new ServiceException(MsoyCodes.E_INTERNAL_ERROR);
            }
        }
        _facebookRepo.saveGameThumbnails(gameId, Lists.transform(thumbnails,
            new Function<FeedThumbnail, FeedThumbnailRecord>() {
                public FeedThumbnailRecord apply (FeedThumbnail thumb) {
                    return FeedThumbnailRecord.forGame(gameId, thumb);
                }
            }));
    }

    @Override // from interface EditGameService
    public List<GameItemEditorInfo> loadGameItems (int gameId, MsoyItemType type)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        GameInfoRecord info = requireIsGameCreator(gameId, mrec);

        ItemRepository<GameItemRecord> repo = _itemLogic.getRepository(GameItemRecord.class, type);

        Map<Integer, GameItemRecord> masters = Maps.newHashMap();
        for (GameItemRecord master : repo.loadGameOriginals(info.gameId)) {
            masters.put(master.catalogId, master);
        }

        List<GameItemEditorInfo> items = Lists.newArrayList();
        for (GameItemRecord original : repo.loadGameOriginals(GameInfo.toDevId(info.gameId))) {
            GameItemEditorInfo itemInfo = new GameItemEditorInfo();
            itemInfo.item = (GameItem)original.toItem();
            itemInfo.listingOutOfDate = original.isListingOutOfDate(
                masters.get(original.catalogId));
            items.add(itemInfo);
        }

        Collections.sort(items, new Comparator<GameItemEditorInfo>() {
            @Override public int compare (GameItemEditorInfo arg0, GameItemEditorInfo arg1) {
                return arg0.item.compareTo(arg1.item);
            }
        });
        return items;
    }

    @Override // from interface EditGameService
    public int createGame (boolean isAVRG, String name, MediaDesc thumbMedia, MediaDesc clientCode)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        GameInfoRecord grec = new GameInfoRecord();
        grec.name = name; // TODO: validate, sigh
        grec.genre = GameGenre.HIDDEN;
        grec.creatorId = mrec.memberId;
        grec.description = "";
        grec.isAVRG = isAVRG;
        grec.thumbMediaHash = HashMediaDesc.unmakeHash(thumbMedia);
        grec.thumbMimeType = MediaMimeTypes.unmakeMimeType(thumbMedia);
        grec.thumbConstraint = MediaDescUtil.unmakeConstraint(thumbMedia);
        grec.blingPool = true;
        _mgameRepo.createGame(grec);

        GameCodeRecord crec = new GameCodeRecord();
        crec.gameId = grec.gameId;
        crec.isDevelopment = true;
        crec.config = "<game>" +
            "<match type=\"0\"><min_seats>1</min_seats><max_seats>1</max_seats></match></game>";
        crec.clientMediaHash = HashMediaDesc.unmakeHash(clientCode);
        crec.clientMimeType = MediaMimeTypes.unmakeMimeType(clientCode);
        _mgameRepo.updateGameCode(crec);

        return grec.gameId;
    }

    @Override // from interface EditGameService
    public void deleteGame (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        GameInfoRecord grec = requireIsGameCreator(gameId, mrec);

        // make sure all of our subitems are delisted and deleted
        int origs = 0, masters = 0;
        for (GameItem item : grec.getSuiteTypes()) {
            ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.getType());
            for (ItemRecord rec : repo.loadGameOriginals(gameId)) {
                if (rec.ownerId != 0) {
                    origs++;
                } else {
                    masters++;
                }
            }
        }
        if (origs > 0) {
            throw new ServiceException("e.must_delete_all_subitems");
        } else if (masters > 0) {
            throw new ServiceException("e.cannot_delete_sold_game");
        }

        // delete this game's comments
        _commentRepo.deleteComments(CommentType.GAME.toByte(), gameId);

        // delete the trophies awarded by this game
        _trophyRepo.purgeGame(gameId);

        // delete game rating and percentile data (single and multiplayer)
        _ratingRepo.purgeGame(-gameId);
        _ratingRepo.purgeGame(gameId);

        _groupRepo.purgeGame(gameId);

        // pass the buck onto the repositories
        _facebookRepo.deleteGameFacebookInfo(gameId);
        _mgameRepo.deleteGame(gameId);
    }

    @Override // from interface EditGameService
    public void updateGameInfo (GameInfo info, boolean blingPool)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        GameInfoRecord grec = requireIsGameCreator(info.gameId, mrec);

        // handle group fiddling
        if (info.groupId != grec.groupId) {
            // member must be a manager of any group they assign to a game
            if (info.groupId != GameInfo.NO_GROUP) {
                if (_groupRepo.getRank(info.groupId, mrec.memberId).compareTo(
                        GroupMembership.Rank.MANAGER) < 0) {
                    throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
                }
                _groupRepo.updateGroupGame(info.groupId, info.gameId);
            }
            // clear out the game from the old group
            if (grec.groupId != GameInfo.NO_GROUP) {
                _groupRepo.updateGroupGame(grec.groupId, 0);
            }
        }

        // write the updated game info record to the repository
        grec.update(info);
        if (mrec.isSupport()) {
            grec.blingPool = blingPool;
        }
        _mgameRepo.updateGameInfo(grec);
    }

    @Override // from interface EditGameService
    public void updateGameCode (GameCode code)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameCreator(code.gameId, mrec);
        if (!code.isDevelopment) {
            log.warning("Refusing update to non-development code.", "who", mrec.who(),
                        "game", code);
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        _mgameRepo.updateGameCode(GameCodeRecord.fromGameCode(code));
        // notify any server hosting this game that its data is updated
        _gameActions.gameUpdated(GameInfo.toDevId(code.gameId));
    }

    @Override // from interface EditGameService
    public void publishGameCode (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameCreator(gameId, mrec);
        _mgameRepo.publishGameCode(gameId);
        // notify any server hosting this game that its data is updated
        _gameActions.gameUpdated(gameId);
    }

    @Override // from interface EditGameService
    public void updateFacebookInfo (FacebookInfo info)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameCreator(info.gameId, mrec);
        _facebookRepo.updateFacebookInfo(FacebookInfoRecord.fromFacebookInfo(info));
    }

    @Override // from interface EditGameService
    public ArcadeEntriesResult loadArcadeEntries (ArcadeData.Portal page)
        throws ServiceException
    {
        requireSupportUser();
        // this is only used for editing, so bypass the cache
        List<ArcadeEntryRecord> entries = _mgameRepo.loadArcadeEntries(page, false);
        ArcadeEntriesResult result = new ArcadeEntriesResult();
        result.entries = Lists.newArrayListWithCapacity(entries.size());
        Set<Integer> featured = Sets.newHashSet();

        for (int ii = 0, ll = entries.size(); ii < ll; ++ii) {
            int gameId = entries.get(ii).gameId;
            int gamePop = getGamePop(_memberMan.getPPSnapshot(), gameId);
            result.entries.add(_mgameRepo.loadGame(gameId).toGameInfo(gamePop));
            if (entries.get(ii).featured) {
                featured.add(gameId);
            }
        }
        result.featured = Lists.newArrayList(featured);
        return result;
    }

    @Override // from interface EditGameService
    public int[] loadArcadeEntryIds (ArcadeData.Portal page)
        throws ServiceException
    {
        requireSupportUser();
        Set<Integer> entryIds = Sets.newHashSet();

        // this is only used for editing, so bypass the cache
        entryIds.addAll(Lists.transform(_mgameRepo.loadArcadeEntries(page, false),
            ArcadeEntryRecord.TO_GAME_ID));
        return Ints.toArray(entryIds);
    }

    @Override // from interface EditGameService
    public void addArcadeEntry (ArcadeData.Portal portal, int gameId)
        throws ServiceException
    {
        requireSupportUser();
        boolean featured = portal == ArcadeData.Portal.MAIN;
        _mgameRepo.addArcadeEntry(portal, gameId, featured);
    }

    @Override // from interface EditGameService
    public void removeArcadeEntry (ArcadeData.Portal portal, int gameId)
        throws ServiceException
    {
        requireSupportUser();
        _mgameRepo.removeArcadeEntry(portal, gameId);
    }

    @Override // from interface EditGameService
    public void updateArcadeEntries (ArcadeData.Portal portal, List<Integer> entries,
        Set<Integer> featured, final Set<Integer> removed)
        throws ServiceException
    {
        requireSupportUser();
        // this is only used for editing, so bypass the cache
        Iterable<ArcadeEntryRecord> old = _mgameRepo.loadArcadeEntries(portal, false);
        if (removed != null) {
            for (int gameId : removed) {
                _mgameRepo.removeArcadeEntry(portal, gameId);
            }
            old = Iterables.filter(old, new Predicate<ArcadeEntryRecord>() {
                public boolean apply (ArcadeEntryRecord rec) {
                   return !removed.contains(rec.gameId);
                }
            });
        }
        if (featured != null) {
            Map<Boolean, ArrayIntSet> toUpdate = ImmutableMap.of(
                true, new ArrayIntSet(), false, new ArrayIntSet());
            for (ArcadeEntryRecord rec : old) {
                if (rec.featured != featured.contains(rec.gameId)) {
                    toUpdate.get(!rec.featured).add(rec.gameId);
                }
            }
            _mgameRepo.updateFeatured(portal, toUpdate.get(true), true);
            _mgameRepo.updateFeatured(portal, toUpdate.get(false), false);
        }
        if (entries != null) {
            _mgameRepo.updateArcadeEntriesOrder(portal, entries);
        }
    }

    @Override // from interface EditGameService
    public List<String> setMochiBucketTags (int bucket, String[] tags)
        throws ServiceException
    {
        requireSupportUser();
        List<String> validated = Lists.newArrayListWithCapacity(tags.length);
        List<String> errors = Lists.newArrayList();
        for (String tag : tags) {
            (_mgameRepo.loadMochiGame(tag) == null && importMochiGame(tag) == null ?
                errors : validated).add(tag);
        }
        if (validated.size() == 0) {
            throw new ServiceException("e.no_mochi_games_found");
        }
        _facebookLogic.setMochiGames(bucket, validated);
        return errors;
    }

    @Override // from interface EditGameService
    public MochiGameBucket getMochiBucket (int bucket)
        throws ServiceException
    {
        requireSupportUser();
        MochiGameBucket mbucket = new MochiGameBucket();
        mbucket.games = Lists.newArrayList(
            _mgameRepo.loadMochiGamesInOrder(_facebookLogic.getMochiGames(bucket)));
        mbucket.current = _facebookLogic.getCurrentGameIndex(bucket);
        return mbucket;
    }

    @Override // from interface EditGameService
    public void testRollFeaturedGames ()
        throws ServiceException
    {
        requireAdminUser();
        // _facebookLogic.testUpdateFeaturedGames();
    }

    protected MochiGameInfo importMochiGame (String mochiTag)
    {
        try {
            URL url = new URL("http://www.mochiads.com/feeds/games/" +
                MOCHI_PUBLISHER_ID + "/" + mochiTag + "/?format=json");
            JSONObject feed = new JSONObject(StreamUtil.toString(url.openStream()));

            JSONObject game = feed.getJSONArray("games").getJSONObject(0);

            // now copy the rough data into our nice record
            MochiGameInfo info = new MochiGameInfo();
            info.name = getJSONStr("name", game, GameInfo.MAX_NAME_LENGTH);
            info.tag = mochiTag;
            info.categories = getJSONStr("categories", game, 255);
            info.author = getJSONStr("author", game, 255);
            info.desc = getJSONStr("description", game, GameInfo.MAX_DESCRIPTION_LENGTH);
            info.thumbURL = getJSONStr("thumbnail_url", game, 255);
            info.swfURL = getJSONStr("swf_url", game, 255);
            info.width = game.getInt("width");
            info.height = game.getInt("height");
            _mgameRepo.addMochiGame(info);
            return info;

        } catch (Exception e) {
            log.warning("Problem adding mochi game.", "tag", mochiTag, e);
            return null;
        }
    }

    /**
     * Utility to get a value from a JSONObject and truncate it.
     */
    protected String getJSONStr (String key, JSONObject obj, int maxLen)
        throws JSONException
    {
        Object v = obj.opt(key);
        String s;
        if (v instanceof String) {
            s = (String)v;
        } else if (v instanceof JSONArray) {
            s = ((JSONArray)v).join(", ");
        } else {
            s = String.valueOf(v);
        }
        return StringUtil.truncate(s, maxLen);
    }

    protected GameInfoRecord requireIsGameCreator (int gameId, MemberRecord mrec)
        throws ServiceException
    {
        // load the source record
        GameInfoRecord grec = _mgameRepo.loadGame(gameId);
        if (grec == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        // verify that the member in question created the game or is support+
        if (grec.creatorId != mrec.memberId && !mrec.isSupport()) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        return grec;
    }

    /** Helpy helper function. */
    protected static int getGamePop (PopularPlacesSnapshot pps, int gameId)
    {
        PopularPlacesSnapshot.Place ppg = pps.getGame(gameId);
        return (ppg == null) ? 0 : ppg.population;
    }

    // our dependencies
    @Inject protected FacebookLogic _facebookLogic;
    @Inject protected FacebookRepository _facebookRepo;
    @Inject protected GameNodeActions _gameActions;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MemberManager _memberMan;
    @Inject protected MsoyGameRepository _mgameRepo;

    // dependencies only needed for deletions
    @Inject protected CommentRepository _commentRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected TrophyRepository _trophyRepo;

    protected static final String MOCHI_PUBLISHER_ID = "709d64407cb1971b";
}
