//
// $Id$

package com.threerings.msoy.game.server;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samskivert.io.StreamUtil;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.Comparators;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.GameItem;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.GameItemRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.TrophySourceRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.comment.server.persist.CommentRepository;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.facebook.server.FacebookLogic;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.FacebookInfo;
import com.threerings.msoy.game.gwt.GameCard;
import com.threerings.msoy.game.gwt.GameCode;
import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameDistribs;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameLogs;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameThumbnail;
import com.threerings.msoy.game.gwt.MochiGameInfo;
import com.threerings.msoy.game.gwt.PlayerRating;
import com.threerings.msoy.game.gwt.TrophyCase;
import com.threerings.msoy.game.server.persist.GameCodeRecord;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.GameThumbnailRecord;
import com.threerings.msoy.game.server.persist.GameTraceLogEnumerationRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.ArcadeEntryRecord;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.HTMLSanitizer;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link GameService}.
 */
public class GameServlet extends MsoyServiceServlet
    implements GameService
{
    // from interface GameService
    public ArcadeData loadArcadeData (ArcadeData.Portal portal)
        throws ServiceException
    {
        // TODO: move all this to PopularPlacesShapshot, why recalculate per request?
        boolean facebook = portal == ArcadeData.Portal.FACEBOOK;

        ArcadeDataBuilder bldr = new ArcadeDataBuilder(portal);
        ArcadeData data = new ArcadeData();

        // top 5 hand-picked featured games
        if (facebook) {
            data.mochiGames = bldr.loadMochiGames();
        } else {
            data.featuredGames = bldr.buildFeatured();
        }

        // list of top 10 or 20 games by ranking
        data.topGames = bldr.buildTopGames(facebook ? 10 : 20);

        // list of the top-200 games alphabetically (only include name and id)
        data.allGames = bldr.buildAllGames();

        if (facebook) {
            // for the facebook redesigned ui, we replace the genres with a "game wall"
            data.gameWall = bldr.buildGameWall(data.topGames.size());

        } else {
            data.genres = bldr.buildGenres();
        }

        return data;
    }

    // from interface GameService
    public List<GameInfo> loadGameGenre (ArcadeData.Portal portal, GameGenre genre, String query)
        throws ServiceException
    {
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
        Set<Integer> filter = portal.isFiltered() ?
            Sets.newHashSet(Lists.transform(_mgameRepo.loadArcadeEntries(portal, true),
                ArcadeEntryRecord.TO_GAME_ID)) : null;
        List<GameInfo> infos = Lists.newArrayList();
        for (GameInfoRecord grec : _mgameRepo.loadGenre(genre, -1, query)) {
            if (filter != null && !filter.contains(grec.gameId)) {
                continue;
            }
            infos.add(grec.toGameInfo(getGamePop(pps, grec.gameId)));
        }
        return infos;
    }

    // from interface GameService
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

    // from interface GameService
    public GameDetail loadGameDetail (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        GameDetail detail = new GameDetail();
        detail.gameId = gameId;

        GameInfoRecord info = _mgameRepo.loadGame(gameId);
        if (info == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        detail.info = info.toGameInfo(getGamePop(_memberMan.getPPSnapshot(), gameId));
        // this should never return null if loadGame() returns non-null
        detail.metrics = _mgameRepo.loadGameMetrics(info.gameId).toGameMetrics();

        // determine how many players can play this game
        int[] players = GameUtil.getMinMaxPlayers(
            detail.info, _mgameRepo.loadGameCode(gameId, false));
        detail.minPlayers = players[0];
        detail.maxPlayers = players[1];

        // fill in other metadata
        detail.info.creator = _memberRepo.loadMemberName(info.creatorId);
        detail.instructions = _mgameRepo.loadInstructions(info.gameId);
        if (mrec != null) {
            detail.memberRating = _mgameRepo.getRatingRepository().getRating(
                info.gameId, mrec.memberId);
        }

        return detail;
    }

    // from interface GameService
    public GameDistribs loadGameMetrics (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameCreator(gameId, mrec);

        GameDistribs metrics = new GameDistribs();
        metrics.gameId = gameId;

        metrics.singleDistribs = Maps.newHashMap();
        for (Map.Entry<Integer, Percentiler> entry :
                 _ratingRepo.loadPercentiles(-gameId).entrySet()) {
            Percentiler tiler = entry.getValue();

            GameDistribs.TilerSummary summary = new GameDistribs.TilerSummary();
            summary.totalCount = tiler.getRecordedCount();
            summary.counts = tiler.getCounts();
            summary.scores = tiler.getRequiredScores();
            summary.maxScore = tiler.getMaxScore();

            metrics.singleDistribs.put(entry.getKey(), summary);
        }

        metrics.multiDistribs = Maps.newHashMap();
        for (Map.Entry<Integer, Percentiler> entry :
                 _ratingRepo.loadPercentiles(gameId).entrySet()) {
            Percentiler tiler = entry.getValue();

            GameDistribs.TilerSummary summary = new GameDistribs.TilerSummary();
            summary.totalCount = tiler.getRecordedCount();
            summary.counts = tiler.getCounts();
            summary.scores = tiler.getRequiredScores();
            summary.maxScore = tiler.getMaxScore();

            metrics.multiDistribs.put(entry.getKey(), summary);
        }

        return metrics;
    }

    // from interface GameService
    public GameLogs loadGameLogs (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameCreator(gameId, mrec);

        GameLogs logs = new GameLogs();
        logs.gameId = gameId;

        List<GameTraceLogEnumerationRecord> records = _mgameRepo.enumerateTraceLogs(gameId);
        logs.logIds = new int[records.size()];
        logs.logTimes = new Date[records.size()];

        for (int ii = 0; ii < records.size(); ii ++) {
            logs.logIds[ii] = records.get(ii).logId;
            logs.logTimes[ii] = new Date(records.get(ii).recorded.getTime());
        }
        return logs;
    }

    // from interface GameService
    public void resetGameScores (int gameId, boolean single, int gameMode)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameCreator(gameId, mrec);

        // wipe the percentiler in the database
        _ratingRepo.deletePercentile(single ? -gameId : gameId, gameMode);

        // tell any resolved instance of this game to clear its in memory percentiler
        _gameActions.resetScores(gameId, single, gameMode);
    }

    // from interface GameService
    public List<Trophy> loadGameTrophies (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        GameInfoRecord grec = _mgameRepo.loadGame(gameId);
        if (grec == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        return loadTrophyInfo(grec, (mrec == null) ? 0 : mrec.memberId, null, null);
    }

    // from interface GameService
    public CompareResult compareTrophies (int gameId, int[] memberIds)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        int callerId = (mrec == null) ? 0 : mrec.memberId;

        GameInfoRecord grec = _mgameRepo.loadGame(gameId);
        if (grec == null) {
            return null;
        }

        CompareResult result = new CompareResult();
        result.gameName = grec.name;
        result.gameThumb = grec.getThumbMedia();

        // load up the trophy and earned information
        result.whenEarneds = new Long[memberIds.length][];
        List<Trophy> trophies = loadTrophyInfo(grec, callerId, memberIds, result.whenEarneds);
        result.trophies = trophies.toArray(new Trophy[trophies.size()]);

        // load up cards for the members in question
        result.members = new MemberCard[memberIds.length];
        for (MemberCardRecord mcr : _memberRepo.loadMemberCards(IntListUtil.asList(memberIds))) {
            result.members[IntListUtil.indexOf(memberIds, mcr.memberId)] = mcr.toMemberCard();
        }

        return result;
    }

    // from interface GameService
    public TrophyCase loadTrophyCase (int memberId)
        throws ServiceException
    {
        MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            return null;
        }

        TrophyCase tcase = new TrophyCase();
        tcase.owner= tgtrec.getName();

        // load the target member's trophies
        ListMultimap<Integer,Trophy> tmap = ArrayListMultimap.create();
        for (TrophyRecord trec : _trophyRepo.loadTrophies(memberId)) {
            tmap.put(trec.gameId, trec.toTrophy());
        }

        // arrange those trophies onto a set of shelves
        Set<Integer> gameIds = tmap.keySet();
        tcase.shelves = new TrophyCase.Shelf[gameIds.size()];
        int ii = 0;
        for (int gameId : gameIds) {
            TrophyCase.Shelf shelf = new TrophyCase.Shelf();
            tcase.shelves[ii++] = shelf;

            shelf.gameId = gameId;
            GameInfoRecord grec = _mgameRepo.loadGame(shelf.gameId);
            if (grec == null) {
                shelf.name = "???"; // the game was delisted or deleted, oh well
            } else {
                shelf.name = grec.name;
            }

            List<Trophy> tlist = tmap.get(gameId);
            shelf.trophies = tlist.toArray(new Trophy[tlist.size()]);
            Arrays.sort(shelf.trophies, new Comparator<Trophy>() {
                    public int compare (Trophy t1, Trophy t2) {
                        return t2.whenEarned.compareTo(t1.whenEarned);
                    }
                });
        }
        // TODO: sort shelves?

        return tcase;
    }

    // from interface GameService
    public PlayerRating[][] loadTopRanked (int gameId, boolean onlyMyFriends)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        if (mrec == null && onlyMyFriends) {
            log.warning("Requested friend rankings for non-authed member [gameId=" + gameId + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // if we should restrict to just this player's friends, figure out who those are
        IntSet friendIds = null;
        if (onlyMyFriends) {
            friendIds = _memberRepo.loadFriendIds(mrec.memberId);
            friendIds.add(mrec.memberId); // us too!
        }

        // if we're showing the full top-ranked list, trim non-recent-players
        long since = onlyMyFriends ? 0L : RATING_CUTOFF;

        // load up the single and mutiplayer ratings
        List<RatingRecord> single =
            _ratingRepo.getTopRatings(-gameId, MAX_RANKINGS, since, friendIds);
        List<RatingRecord> multi =
            _ratingRepo.getTopRatings(gameId, MAX_RANKINGS, since, friendIds);

        // combine all players in question into one map for name/photo resolution
        IntMap<PlayerRating> players = IntMaps.newHashIntMap();
        for (RatingRecord record : single) {
            players.put(record.playerId, new PlayerRating());
        }
        for (RatingRecord record : multi) {
            players.put(record.playerId, new PlayerRating());
        }

        // resolve the member's names
        Set<Integer> memIds = players.keySet();
        for (MemberName name : _memberRepo.loadMemberNames(memIds).values()) {
            PlayerRating pr = players.get(name.getMemberId());
            pr.name = name;
        }

        // resolve their profile photos
        for (ProfileRecord profile : _profileRepo.loadProfiles(memIds)) {
            PlayerRating pr = players.get(profile.memberId);
            pr.photo = profile.getPhoto();
        }

        // create our result arrays and fill in their actual ratings
        return new PlayerRating[][] { toRatingResult(single, players),
                                      toRatingResult(multi, players) };
    }

    // from interface GameService
    public RatingResult rateGame (int gameId, byte rating)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        return _mgameRepo.getRatingRepository().rate(gameId, mrec.memberId, rating).left;
    }

    // from interface GameService
    public GameData loadGameData (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        GameInfoRecord info = requireIsGameCreator(gameId, mrec);
        GameData data = new GameData();
        data.info = info.toGameInfo(0);
        data.facebook = _mgameRepo.loadFacebookInfo(info.gameId);
        data.devCode = _mgameRepo.loadGameCode(GameInfo.toDevId(info.gameId), false);
        data.pubCode = _mgameRepo.loadGameCode(info.gameId, false);
        return data;
    }

    // from interface GameService
    public List<GameThumbnail> loadThumbnails (int gameId)
        throws ServiceException
    {
        requireAuthedUser();
        return Lists.newArrayList(Lists.transform(_mgameRepo.loadAllThumbnails(gameId),
            new Function<GameThumbnailRecord, GameThumbnail> () {
            public GameThumbnail apply (GameThumbnailRecord rec) {
                return rec.toGameThumbnail();
            }
        }));
    }

    // from interface GameService
    public void updateThumbnails (final int gameId, List<GameThumbnail> thumbnails)
        throws ServiceException
    {
        MemberRecord mrec;
        if (gameId == 0) {
            mrec = requireAdminUser();
        } else {
            mrec = requireAuthedUser();
            requireIsGameCreator(gameId, mrec);
        }
        for (GameThumbnail thumb : thumbnails) {
            if (thumb.media == null || !thumb.media.isImage()) {
                // this should never happen with a legitimate client
                throw new ServiceException(MsoyCodes.E_INTERNAL_ERROR);
            }
            if (gameId != 0 && !thumb.type.gameOverridable) {
                // this should never happen with a legitimate client
                throw new ServiceException(MsoyCodes.E_INTERNAL_ERROR);
            }
        }
        _mgameRepo.saveThumbnails(gameId, Lists.transform(thumbnails,
            new Function<GameThumbnail, GameThumbnailRecord>() {
                public GameThumbnailRecord apply (GameThumbnail thumb) {
                    return new GameThumbnailRecord(gameId, thumb);
                }
            }));
    }

    // from interface GameService
    public List<GameItemEditorInfo> loadGameItems (int gameId, byte type)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        GameInfoRecord info = requireIsGameCreator(gameId, mrec);

        ItemRepository<GameItemRecord> repo = _itemLogic.getRepository(GameItemRecord.class, type);

        IntMap<GameItemRecord> masters = IntMaps.newHashIntMap();
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

    // from interface GameService
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
        grec.thumbMediaHash = MediaDesc.unmakeHash(thumbMedia);
        grec.thumbMimeType = MediaDesc.unmakeMimeType(thumbMedia);
        grec.thumbConstraint = MediaDesc.unmakeConstraint(thumbMedia);
        _mgameRepo.createGame(grec);

        GameCodeRecord crec = new GameCodeRecord();
        crec.gameId = grec.gameId;
        crec.isDevelopment = true;
        crec.config = "<game>" +
            "<match type=\"0\"><min_seats>1</min_seats><max_seats>1</max_seats></match></game>";
        crec.clientMediaHash = MediaDesc.unmakeHash(clientCode);
        crec.clientMimeType = MediaDesc.unmakeMimeType(clientCode);
        _mgameRepo.updateGameCode(crec);

        return grec.gameId;
    }

    // from interface GameService
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
        _commentRepo.deleteComments(Comment.TYPE_GAME, gameId);

        // delete the trophies awarded by this game
        _trophyRepo.purgeGame(gameId);

        // delete game rating and percentile data (single and multiplayer)
        _ratingRepo.purgeGame(-gameId);
        _ratingRepo.purgeGame(gameId);

        // pass the buck onto the repository
        _mgameRepo.deleteGame(gameId);
    }

    // from interface GameService
    public void updateGameInfo (GameInfo info)
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
        _mgameRepo.updateGameInfo(grec);
    }

    // from interface GameService
    public void updateGameInstructions (int gameId, String instructions)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameCreator(gameId, mrec);

        // trust not the user; they are prone to evil
        instructions = HTMLSanitizer.sanitize(instructions);

        // now that we've confirmed that they're allowed, update the instructions
        _mgameRepo.updateInstructions(gameId, instructions);
    }

    // from interface GameService
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

    // from interface GameService
    public void publishGameCode (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameCreator(gameId, mrec);
        _mgameRepo.publishGameCode(gameId);
        // notify any server hosting this game that its data is updated
        _gameActions.gameUpdated(gameId);
    }

    // from interface GameService
    public void updateFacebookInfo (FacebookInfo info)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameCreator(info.gameId, mrec);
        _mgameRepo.updateFacebookInfo(info);
    }

    // from interface GameService
    public ArcadeEntriesResult loadArcadeEntries (ArcadeData.Portal page)
        throws ServiceException
    {
        requireSupportUser();
        // this is only used for editing, so bypass the cache
        List<ArcadeEntryRecord> entries = _mgameRepo.loadArcadeEntries(page, false);
        ArcadeEntriesResult result = new ArcadeEntriesResult();
        result.entries = Lists.newArrayListWithCapacity(entries.size());
        ArrayIntSet featured = new ArrayIntSet();
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

    // from interface GameService
    public int[] loadArcadeEntryIds (ArcadeData.Portal page)
        throws ServiceException
    {
        requireSupportUser();
        ArrayIntSet entryIds = new ArrayIntSet();
        // this is only used for editing, so bypass the cache
        entryIds.addAll(Lists.transform(_mgameRepo.loadArcadeEntries(page, false),
            ArcadeEntryRecord.TO_GAME_ID));
        return entryIds.toIntArray();
    }

    // from interface GameService
    public void addArcadeEntry (ArcadeData.Portal portal, int gameId)
        throws ServiceException
    {
        requireSupportUser();
        boolean featured = portal == ArcadeData.Portal.MAIN;
        _mgameRepo.addArcadeEntry(portal, gameId, featured);
    }

    // from interface GameService
    public void removeArcadeEntry (ArcadeData.Portal portal, int gameId)
        throws ServiceException
    {
        requireSupportUser();
        _mgameRepo.removeArcadeEntry(portal, gameId);
    }

    // from interface GameService
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

    // from interface GameService
    public MochiGameInfo getMochiGame (String mochiTag)
        throws ServiceException
    {
        MemberRecord member = getAuthedUser();
        // make sure we got something before we log something
        MochiGameInfo info = _mgameRepo.loadMochiGame(mochiTag);
        if (info == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        _eventLog.facebookMochiGameEntered((member != null) ? member.memberId : 0, mochiTag);
        return info;
    }

    @Override // from GameService
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

    @Override // from GameService
    public MochiGameBucket getMochiBucket (int bucket)
        throws ServiceException
    {
        requireSupportUser();
        MochiGameBucket mbucket = new MochiGameBucket();
        mbucket.games = Lists.newArrayList(
            _mgameRepo.loadMochiGamesInOrder(_facebookLogic.getMochiGames(bucket)));
        mbucket.currentTag = _facebookLogic.getCurrentGame(bucket);
        return mbucket;
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

    protected PlayerRating[] toRatingResult (
        List<RatingRecord> records, IntMap<PlayerRating> players)
    {
        PlayerRating[] result = new PlayerRating[records.size()];
        for (int ii = 0; ii < result.length; ii++) {
            RatingRecord record = records.get(ii);
            result[ii] = new PlayerRating();
            result[ii].name = players.get(record.playerId).name;
            result[ii].photo = players.get(record.playerId).photo;
            result[ii].rating = record.rating;
        }
        return result;
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

    /**
     * Helper function for {@link #loadGameTrophies} and {@link #compareTrophies}.
     */
    protected List<Trophy> loadTrophyInfo (GameInfoRecord grec, int callerId,
                                           int[] earnerIds, Long[][] whenEarneds)
        throws ServiceException
    {
        TrophySourceRepository tsrepo = _trophySourceRepo;

        // load up the (listed) trophy source records for this game
        Map<String,Trophy> trophies = Maps.newHashMap();
        List<TrophySourceRecord> trecords = tsrepo.loadGameOriginals(grec.gameId);
        for (TrophySourceRecord record : trecords) {
            Trophy trophy = new Trophy();
            trophy.gameId = grec.gameId;
            trophy.name = record.name;
            trophy.ident = record.ident;
            trophy.trophyMedia = new MediaDesc(record.thumbMediaHash, record.thumbMimeType);
            trophies.put(record.ident, trophy);
        }

        // fill in earned dates for the caller if one was specified
        if (callerId != 0) {
            for (TrophyRecord record : _trophyRepo.loadTrophies(grec.gameId, callerId)) {
                Trophy trophy = trophies.get(record.ident);
                if (trophy != null) {
                    trophy.whenEarned = record.whenEarned.getTime();
                }
            }
        }

        // sort the trophies in the creator's desired order
        Collections.sort(trecords, TrophySourceRecord.BY_SORT_ORDER);

        // populate the result lists in the correct order
        List<Trophy> results = Lists.newArrayList();
        for (TrophySourceRecord record : trecords) {
            Trophy trophy = trophies.get(record.ident);
            // only provide the description for non-secret or earned trophies
            if (!record.secret || trophy.whenEarned != null) {
                trophy.description = record.description;
            }
            results.add(trophy);
        }

        // if we also want to load when earned info for other players, do so
        int ecount = (earnerIds != null) ? earnerIds.length : 0;
        for (int ee = 0; ee < ecount; ee++) {
            Map<String,Long> earned = Maps.newHashMap();
            int earnerId = earnerIds[ee];
            if (earnerId == callerId) {
                // if this earner is the caller, we already have their earned times
                for (TrophySourceRecord record : trecords) {
                    earned.put(record.ident, trophies.get(record.ident).whenEarned);
                }
            } else {
                for (TrophyRecord record : _trophyRepo.loadTrophies(grec.gameId, earnerId)) {
                    earned.put(record.ident, record.whenEarned.getTime());
                }
            }

            Long[] whenEarned = new Long[results.size()];
            for (int tt = 0; tt < whenEarned.length; tt++) {
                whenEarned[tt] = earned.get(trecords.get(tt).ident);
            }
            whenEarneds[ee] = whenEarned;
        }

        return results;
    }

    protected class ArcadeDataBuilder
    {
        public ArcadeDataBuilder (ArcadeData.Portal portal)
        {
            _portal = portal;
            // load the hand-picked "top games"
            _agames = _mgameRepo.loadArcadeEntries(portal, true);

            // set up "approved" set for some portals
            Set<Integer> approvedGames = null;
            if (portal.isFiltered()) {
                approvedGames = Sets.newHashSet(
                    Lists.transform(_agames, ArcadeEntryRecord.TO_GAME_ID));
            }

            // load the top N (where N is large) games and build everything from that list
            _games = Maps.newLinkedHashMap();
            for (GameInfoRecord grec : _mgameRepo.loadGenre(GameGenre.ALL, ARCADE_RAW_COUNT)) {
                // for filtered arcade portals, filter the whole map
                if (approvedGames == null || approvedGames.contains(grec.gameId)) {
                    _games.put(grec.gameId, grec);
                }
            }
        }

        /**
         * TEMP
         */
        public MochiGameInfo[] loadMochiGames ()
        {
            List<MochiGameInfo> games = _mgameRepo.loadMochiGamesInOrder(
                _facebookLogic.getFeaturedGames());
            return games.toArray(new MochiGameInfo[games.size()]);
        }

        public GameInfo[] buildFeatured ()
        {
            ArrayIntSet creatorIds = new ArrayIntSet();
            List<GameInfo> featured = Lists.newArrayList();
            for (ArcadeEntryRecord topGame : _agames) {
                if (!topGame.featured) {
                    continue;
                }
                int gameId = topGame.gameId;
                GameInfoRecord info = _games.get(gameId);
                if (info == null) {
                    continue;
                }
                featured.add(info.toGameInfo(getGamePop(_pps, gameId)));
                creatorIds.add(info.creatorId);
                if (featured.size() == ArcadeData.FEATURED_GAME_COUNT) {
                    break;
                }
            }

            // resolve creator names
            IntMap<MemberName> memberNames = _memberRepo.loadMemberNames(creatorIds);
            for (GameInfo info : featured) {
                info.creator = memberNames.get(info.creator.getMemberId());
            }
            return featured.toArray(new GameInfo[featured.size()]);
        }

        public List<GameCard> buildTopGames (int topGameCount)
        {
            List<GameCard> topGames = Lists.newArrayList();
            for (GameInfoRecord game : _games.values()) {
                topGames.add(game.toGameCard(getGamePop(_pps, game.gameId)));
                if (topGames.size() == topGameCount) {
                    break;
                }
            }
            return topGames;
        }

        public List<GameCard> buildAllGames ()
        {
            List<GameCard> allGames = Lists.newArrayList();
            for (GameInfoRecord game : _games.values()) {
                allGames.add(game.toGameCard(0)); // playersOnline not needed here
            }
            Collections.sort(allGames, GameCard.BY_NAME);
            return allGames;
        }

        public List<ArcadeData.Genre> buildGenres ()
        {
            // load up our genre counts
            Map<GameGenre, Integer> genreCounts = _mgameRepo.loadGenreCounts();

            // load information about the genres
            List<ArcadeData.Genre> genres = Lists.newArrayList();
            for (GameGenre gcode : GameGenre.DISPLAY_GENRES) {
                ArcadeData.Genre genre = new ArcadeData.Genre();
                genre.genre = gcode;
                if (genreCounts.containsKey(gcode)) {
                    genre.gameCount = genreCounts.get(gcode);
                }
                if (genre.gameCount == 0) {
                    continue;
                }

                // filter out all the games in this genre
                int max = 3 * ArcadeData.Genre.HIGHLIGHTED_GAMES;
                List<GameCard> ggames = Lists.newArrayListWithCapacity(max);
                for (GameInfoRecord grec : _games.values()) {
                    // games rated less than 3 don't get on the main page
                    if (grec.genre == gcode && grec.getRating() >= MIN_ARCADE_RATING) {
                        ggames.add(grec.toGameCard(getGamePop(_pps, grec.gameId)));
                        // stop when we've got 3*HIGHLIGHTED_GAMES
                        if (ggames.size() == max) {
                            break;
                        }
                    }
                }

                // shuffle those and then sort them by players online
                Collections.shuffle(ggames);
                Collections.sort(ggames, new Comparator<GameCard>() {
                    public int compare (GameCard one, GameCard two) {
                        return Comparators.compare(two.playersOnline, one.playersOnline);
                    }
                });
                // finally take N from that shuffled list as the games to show
                CollectionUtil.limit(ggames, ArcadeData.Genre.HIGHLIGHTED_GAMES);
                genre.games = ggames.toArray(new GameCard[ggames.size()]);

                genres.add(genre);
            }
            return genres;
        }

        public List<GameCard> buildGameWall (int topGameCount)
        {
            // load up games not in the top N
            List<GameInfoRecord> remainder =
                Lists.newArrayListWithExpectedSize(_games.size() - topGameCount);
            int ii = 0;
            for (GameInfoRecord game : _games.values()) {
                if (ii++ >= topGameCount) {
                    remainder.add(game);
                }
            }

            // first 5 of 20 are randomly selected from the top 10 of the remainder
            List<GameInfoRecord> top10 = remainder.subList(0, Math.min(remainder.size(), 10));
            List<GameInfoRecord> wall = removeAndShuffleRandomSubset(top10, 5);

            // the last 15 are random
            wall.addAll(removeAndShuffleRandomSubset(remainder, 15));

            // convert to cards
            List<GameCard> result = Lists.newArrayListWithExpectedSize(wall.size());
            for (GameInfoRecord grec : wall) {
                result.add(grec.toGameCard(getGamePop(_pps, grec.gameId)));
            }
            return result;
        }

        protected List<ArcadeEntryRecord> _agames;
        protected Map<Integer, GameInfoRecord> _games;
        protected PopularPlacesSnapshot _pps = _memberMan.getPPSnapshot();
        protected ArcadeData _data = new ArcadeData();
        protected ArcadeData.Portal _portal;
    }

    /** Helpy helper function. */
    protected static int getGamePop (PopularPlacesSnapshot pps, int gameId)
    {
        PopularPlacesSnapshot.Place ppg = pps.getGame(gameId);
        return (ppg == null) ? 0 : ppg.population;
    }

    /**
     * Builds a list of elements of a given collection by removing random ones, up to a given size.
     * Shuffles the result. Copied from {@link CollectionUtil#selectRandomSubset()} and modified.
     */
    protected static <T> List<T> removeAndShuffleRandomSubset (Collection<T> col, int count)
    {
        int csize = col.size();
        ArrayList<T> subset = new ArrayList<T>(count);
        Iterator<T> iter = col.iterator();
        int s = 0;

        for (int k = 0; iter.hasNext(); k++) {
            T elem = iter.next();
            float limit = ((float)(count - s)) / ((float)(csize - k));
            if (Math.random() < limit) {
                subset.add(elem);
                iter.remove();
                if (++s == count) {
                    break;
                }
            }
        }

        Collections.shuffle(subset);
        return subset;
    }

    // our dependencies
    @Inject protected CommentRepository _commentRepo;
    @Inject protected FacebookLogic _facebookLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameNodeActions _gameActions;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MemberManager _memberMan;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected TrophyRepository _trophyRepo;
    @Inject protected TrophySourceRepository _trophySourceRepo;

    protected static final int MAX_RANKINGS = 10;
    protected static final int ARCADE_RAW_COUNT = 200;
    protected static final float MIN_ARCADE_RATING = 3.0f;

    protected static final String MOCHI_PUBLISHER_ID = "709d64407cb1971b";

    /** Players that haven't played a rated game in 14 days are not included in top-ranked. */
    protected static final long RATING_CUTOFF = 14 * 24*60*60*1000L;
}
