//
// $Id$

package com.threerings.msoy.game.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.Comparators;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.TrophySourceRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

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
import com.threerings.msoy.game.gwt.PlayerRating;
import com.threerings.msoy.game.gwt.TrophyCase;
import com.threerings.msoy.game.server.persist.GameCodeRecord;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.GameTraceLogEnumerationRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.server.MemberManager;
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
    public ArcadeData loadArcadeData ()
        throws ServiceException
    {
        ArcadeData data = new ArcadeData();
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();

        // load the top N (where N is large) games and build everything from that list
        Map<Integer, GameInfoRecord> games = Maps.newLinkedHashMap();
        for (GameInfoRecord grec : _mgameRepo.loadGenre(GameGenre.ALL, ARCADE_RAW_COUNT)) {
            games.put(grec.gameId, grec);
        }

        // determine the "featured" games
        data.featuredGames = _gameLogic.loadTopGames(pps, false);

        // list of the top-200 games alphabetically (only include name and id)
        data.allGames = Lists.newArrayList();
        for (GameInfoRecord game : games.values()) {
            data.allGames.add(game.toGameCard(0)); // playersOnline not needed here
        }
        Collections.sort(data.allGames, GameCard.BY_NAME);

        // list of top N games by ranking
        data.topGames = Lists.newArrayList();
        for (GameInfoRecord game : games.values()) {
            data.topGames.add(game.toGameCard(getGamePop(pps, game.gameId)));
            if (data.topGames.size() == ArcadeData.TOP_GAME_COUNT) {
                break;
            }
        }

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
            for (GameInfoRecord grec : games.values()) {
                // games rated less than 3 don't get on the main page
                if (grec.genre == gcode && grec.getRating() >= MIN_ARCADE_RATING) {
                    ggames.add(grec.toGameCard(getGamePop(pps, grec.gameId)));
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
        data.genres = genres;

        return data;
    }

    // from interface GameService
    public List<GameInfo> loadGameGenre (GameGenre genre, String query)
        throws ServiceException
    {
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
        List<GameInfo> infos = Lists.newArrayList();
        for (GameInfoRecord grec : _mgameRepo.loadGenre(genre, -1, query)) {
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
    public List<Item> loadGameItems (int gameId, byte type)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        GameInfoRecord info = requireIsGameCreator(gameId, mrec);

        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        List<Item> items = Lists.newArrayList();
        items.addAll(Lists.transform(repo.loadGameOriginals(GameInfo.toDevId(info.gameId)),
                                     new ItemRecord.ToItem<Item>()));
        Collections.sort(items);
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
        crec.config = "";
        crec.clientMediaHash = MediaDesc.unmakeHash(clientCode);
        crec.clientMimeType = MediaDesc.unmakeMimeType(clientCode);
        _mgameRepo.updateGameCode(crec);

        return grec.gameId;
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

    /** Helpy helper function. */
    protected static int getGamePop (PopularPlacesSnapshot pps, int gameId)
    {
        PopularPlacesSnapshot.Place ppg = pps.getGame(gameId);
        return (ppg == null) ? 0 : ppg.population;
    }

    // our dependencies
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameNodeActions _gameActions;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MemberManager _memberMan;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected TrophyRepository _trophyRepo;
    @Inject protected TrophySourceRepository _trophySourceRepo;

    protected static final int MAX_RANKINGS = 10;
    protected static final int ARCADE_RAW_COUNT = 200;
    protected static final float MIN_ARCADE_RATING = 3.0f;

    /** Players that haven't played a rated game in 14 days are not included in top-ranked. */
    protected static final long RATING_CUTOFF = 14 * 24*60*60*1000L;
}
