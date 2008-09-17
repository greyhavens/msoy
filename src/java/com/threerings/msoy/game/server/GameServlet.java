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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Comparators;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.GameDetailRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.GameTraceLogEnumerationRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.peer.server.GameNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.FeaturedGameInfo;
import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameLogs;
import com.threerings.msoy.game.gwt.GameMetrics;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.PlayerRating;
import com.threerings.msoy.game.gwt.TrophyCase;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.HTMLSanitizer;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link GameService}.
 */
public class GameServlet extends MsoyServiceServlet
    implements GameService
{
    // from interface GameService
    public GameDetail loadGameDetail (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        GameDetailRecord gdr = _gameRepo.loadGameDetail(gameId);
        if (gdr == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        GameDetail detail = gdr.toGameDetail();
        int creatorId = 0;
        if (gdr.sourceItemId != 0) {
            ItemRecord item = _gameRepo.loadItem(gdr.sourceItemId);
            if (item != null) {
                detail.sourceItem = (Game)item.toItem();
                creatorId = item.creatorId;
            }
        }

        if (detail.sourceItem == null) {
            log.warning("Game has no source item", "gameId", gameId);
        }

        detail.instructions = _gameRepo.loadInstructions(gdr.gameId);

        if (gdr.listedItemId != 0) {
            ItemRecord item = _gameRepo.loadItem(gdr.listedItemId);
            if (item != null) {
                detail.listedItem = (Game)item.toItem();
                creatorId = item.creatorId;
                detail.memberItemInfo = _itemLogic.getMemberItemInfo(mrec, detail.listedItem);
            }
        }

        if (creatorId != 0) {
            detail.creator = _memberRepo.loadMemberName(creatorId);
        }

        PopularPlacesSnapshot.Place game = _memberMan.getPPSnapshot().getGame(gameId);
        if (game != null) {
            detail.playingNow = game.population;
        }

        // determine how many players can play this game
        Game item = Game.isDevelopmentVersion(gameId) ? detail.sourceItem : detail.listedItem;
        if (item == null) {
            log.warning("Game has no item", "gameId", gameId, "detail", detail);
        }
        int[] players = GameUtil.getMinMaxPlayers(item);
        detail.minPlayers = players[0];
        detail.maxPlayers = players[1];

        return detail;
    }

    // from interface GameService
    public GameMetrics loadGameMetrics (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameOwner(gameId, mrec);

        GameMetrics metrics = new GameMetrics();
        metrics.gameId = gameId;

        Percentiler stiler = _ratingRepo.loadPercentile(-gameId);
        if (stiler != null) {
            metrics.singleTotalCount = stiler.getRecordedCount();
            metrics.singleCounts = stiler.getCounts();
            metrics.singleScores = stiler.getRequiredScores();
            metrics.singleMaxScore = stiler.getMaxScore();
        }

        Percentiler mtiler = _ratingRepo.loadPercentile(gameId);
        if (stiler != null) {
            metrics.multiTotalCount = mtiler.getRecordedCount();
            metrics.multiCounts = mtiler.getCounts();
            metrics.multiScores = mtiler.getRequiredScores();
            metrics.multiMaxScore = mtiler.getMaxScore();
        }

        return metrics;
    }

    // from interface GameService
    public GameLogs loadGameLogs (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameOwner(Game.getListedId(gameId), mrec);

        GameLogs logs = new GameLogs();
        logs.gameId = gameId;

        List<GameTraceLogEnumerationRecord> records = _gameRepo.enumerateTraceLogs(gameId);
        logs.logIds = new int[records.size()];
        logs.logTimes = new Date[records.size()];

        for (int ii = 0; ii < records.size(); ii ++) {
            logs.logIds[ii] = records.get(ii).logId;
            logs.logTimes[ii] = new Date(records.get(ii).recorded.getTime());
        }
        return logs;
    }

    // from interface GameService
    public void updateGameInstructions (int gameId, String instructions)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameOwner(gameId, mrec);

        // trust not the user; they are prone to evil
        instructions = HTMLSanitizer.sanitize(instructions);

        // now that we've confirmed that they're allowed, update the instructions
        _gameRepo.updateInstructions(gameId, instructions);
    }

    // from interface GameService
    public void resetGameScores (int gameId, boolean single)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        requireIsGameOwner(gameId, mrec);

        // wipe the percentiler in the database
        int uGameId = single ? -gameId : gameId;
        _ratingRepo.updatePercentile(uGameId, new Percentiler());

        // tell any resolved instance of this game to clear its in memory percentiler
        _peerMan.invokeNodeAction(new ResetScoresAction(gameId, single));
    }

    // from interface GameService
    public List<Trophy> loadGameTrophies (int gameId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        GameRecord grec = _gameRepo.loadGameRecord(gameId);
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

        GameRecord grec = _gameRepo.loadGameRecord(gameId);
        if (grec == null) {
            return null;
        }

        CompareResult result = new CompareResult();
        result.gameName = grec.name;
        result.gameThumb = grec.getThumbMediaDesc();

        // load up the trophy and earned information
        result.whenEarneds = new Long[memberIds.length][];
        List<Trophy> trophies = loadTrophyInfo(grec, callerId, memberIds, result.whenEarneds);
        result.trophies = trophies.toArray(new Trophy[trophies.size()]);

        // load up cards for the members in question
        result.members = new MemberCard[memberIds.length];
        for (MemberCardRecord mcr : _memberRepo.loadMemberCards(new ArrayIntSet(memberIds))) {
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
        ListMultimap<Integer,Trophy> tmap = Multimaps.newArrayListMultimap();
        for (TrophyRecord trec : _trophyRepo.loadTrophies(memberId)) {
            tmap.put(trec.gameId, trec.toTrophy());
        }

        // arrange those trophies onto a set of shelves
        tcase.shelves = new TrophyCase.Shelf[tmap.size()];
        int ii = 0;
        for (int gameId : tmap.keySet()) {
            TrophyCase.Shelf shelf = new TrophyCase.Shelf();
            tcase.shelves[ii++] = shelf;

            shelf.gameId = gameId;
            GameRecord grec = _gameRepo.loadGameRecord(shelf.gameId);
            if (grec == null) {
                log.warning("Have trophies for unknown game [who=" + memberId +
                            ", gameId=" + shelf.gameId + "].");
                shelf.name = "???";
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
    public ArcadeData loadArcadeData ()
        throws ServiceException
    {
        ArcadeData data = new ArcadeData();
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();

        // load the top N (where N is large) games and build everything from that list
        Map<Integer, GameRecord> games = Maps.newLinkedHashMap();
        for (GameRecord grec : _gameRepo.loadGenre((byte)-1, ARCADE_RAW_COUNT)) {
            games.put(grec.gameId, grec);
        }

        // determine the "featured" games
        List<FeaturedGameInfo> featured = Lists.newArrayList();
        ArrayIntSet have = new ArrayIntSet();
        for (PopularPlacesSnapshot.Place card : pps.getTopGames()) {
            GameDetailRecord detail = _gameRepo.loadGameDetail(card.placeId);
            // popular places never has in-development games
            GameRecord game = games.get(detail.listedItemId);
            if (game == null) {
                game = _gameRepo.loadItem(detail.listedItemId);
            }
            if (game != null) {
                featured.add(_gameLogic.toFeaturedGameInfo(game, detail, card.population));
                have.add(game.gameId);
            }
            if (featured.size() == ArcadeData.FEATURED_GAME_COUNT) {
                break;
            }
        }
        if (featured.size() < ArcadeData.FEATURED_GAME_COUNT) {
            for (GameRecord game : games.values()) {
                if (!have.contains(game.gameId)) {
                    GameDetailRecord detail = _gameRepo.loadGameDetail(game.gameId);
                    featured.add(_gameLogic.toFeaturedGameInfo(game, detail, 0));
                }
                if (featured.size() == ArcadeData.FEATURED_GAME_COUNT) {
                    break;
                }
            }
        }
        data.featuredGames = featured.toArray(new FeaturedGameInfo[featured.size()]);

        // list of the top-200 games alphabetically (only include name and id)
        data.allGames = Lists.newArrayList();
        for (GameRecord game : games.values()) {
            GameInfo gameInfo = new GameInfo();
            gameInfo.gameId = game.gameId;
            gameInfo.name = game.name;
            data.allGames.add(gameInfo);
        }
        Collections.sort(data.allGames, SORT_BY_NAME);

        // list of top 10 games by ranking (include name, id & media)
        data.topGames = Lists.newArrayList();
        for (GameRecord game : games.values()) {
            GameInfo gameInfo = new GameInfo();
            // we only want some of the game info here, so we don't use GameRecord.toGameInfo
            gameInfo.gameId = game.gameId;
            gameInfo.name = game.name;
            gameInfo.thumbMedia = game.getThumbMediaDesc();
            data.topGames.add(gameInfo);
            if (data.topGames.size() == ArcadeData.TOP_GAME_COUNT) {
                break;
            }
        }

        // load up our genre counts
        IntIntMap genreCounts = _gameRepo.loadGenreCounts();

        // load information about the genres
        List<ArcadeData.Genre> genres = Lists.newArrayList();
        for (byte gcode : Game.GENRES) {
            ArcadeData.Genre genre = new ArcadeData.Genre();
            genre.genre = gcode;
            genre.gameCount = Math.max(0, genreCounts.get(gcode));
            if (genre.gameCount == 0) {
                continue;
            }

            // filter out all the games in this genre
            List<GameInfo> ggames = Lists.newArrayList();
            for (GameRecord grec : games.values()) {
                if (grec.genre == gcode) {
                    GameInfo info = grec.toGameInfo();
                    PopularPlacesSnapshot.Place ppg = pps.getGame(grec.gameId);
                    if (ppg != null) {
                        info.playersOnline = ppg.population;
                    }
                    ggames.add(info);
                }
            }

            // shuffle those and then sort them by players online
            Collections.shuffle(ggames);
            Collections.sort(ggames, new Comparator<GameInfo>() {
                public int compare (GameInfo one, GameInfo two) {
                    return Comparators.compare(two.playersOnline, one.playersOnline);
                }
            });

            // finally take N from that shuffled list as the games to show
            List<GameInfo> hgames = ggames.subList(
                0, Math.min(ggames.size(), ArcadeData.Genre.HIGHLIGHTED_GAMES));
            genre.games = hgames.toArray(new GameInfo[hgames.size()]);

            genres.add(genre);
        }
        data.genres = genres;

        return data;
    }

    // from interface GameService
    public List<GameInfo> loadGameGenre (byte genre, byte sortMethod, String query)
        throws ServiceException
    {
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();

        // load up all the games in this genre
        List<GameRecord> games = _gameRepo.loadGenre(genre, -1, query);

        // convert them to game info objects
        List<GameInfo> infos = Lists.newArrayList();
        for (GameRecord grec : games) {
            GameInfo info = grec.toGameInfo();
            // add the players online
            PopularPlacesSnapshot.Place gameCard = pps.getGame(grec.gameId);
            if (gameCard != null) {
                info.playersOnline = gameCard.population;
            }
            infos.add(info);
        }

        // sort by the preferred sort method (sorted by rating within groups)
        if (sortMethod == GameInfo.SORT_BY_RATING) {
            // do nothing, this is the default from the repository
        } else if (sortMethod == GameInfo.SORT_BY_NEWEST) {
            Collections.sort(infos, SORT_BY_NEWEST);
        } else if (sortMethod == GameInfo.SORT_BY_NAME) {
            Collections.sort(infos, SORT_BY_NAME);
        } else if (sortMethod == GameInfo.SORT_BY_MULTIPLAYER) {
            Collections.sort(infos, SORT_BY_MULTIPLAYER);
        } else if (sortMethod == GameInfo.SORT_BY_SINGLE_PLAYER) {
            Collections.sort(infos, SORT_BY_SINGLE_PLAYER);
        } else if (sortMethod == GameInfo.SORT_BY_GENRE) {
            Collections.sort(infos, SORT_BY_GENRE);
        } else if (sortMethod == GameInfo.SORT_BY_PLAYERS_ONLINE) {
            Collections.sort(infos, SORT_BY_PLAYERS_ONLINE);
        }

        return infos;
    }

    // from interface GameService
    public FeaturedGameInfo[] loadTopGamesData ()
        throws ServiceException
    {
        return _gameLogic.loadTopGames(_memberMan.getPPSnapshot());
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

    protected void requireIsGameOwner (int gameId, MemberRecord mrec)
        throws ServiceException
    {
        // load the source record
        GameRecord grec = _gameRepo.loadGameRecord(Game.getDevelopmentId(gameId));
        if (grec == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        // verify that the member in question owns the game or is an admin
        if (grec.ownerId != mrec.memberId && !mrec.isAdmin()) {
            throw new ServiceException(InvocationCodes.E_ACCESS_DENIED);
        }
    }

    /**
     * Helper function for {@link #loadGameTrophies} and {@link #compareTrophies}.
     */
    protected List<Trophy> loadTrophyInfo (GameRecord grec, int callerId,
                                           int[] earnerIds, Long[][] whenEarneds)
        throws ServiceException
    {
        TrophySourceRepository tsrepo = _trophySourceRepo;

        int gameSuiteId = grec.toItem().getSuiteId();

        // load up the (listed) trophy source records for this game
        Map<String,Trophy> trophies = Maps.newHashMap();
        List<TrophySourceRecord> trecords = tsrepo.loadOriginalItemsBySuite(gameSuiteId);
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
        Collections.sort(trecords, new Comparator<TrophySourceRecord>() {
                public int compare (TrophySourceRecord t1, TrophySourceRecord t2) {
                    return t1.sortOrder - t2.sortOrder;
                }
            });

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

    /** Used by {@link #resetGameScores}. */
    protected static class ResetScoresAction extends GameNodeAction
    {
        public ResetScoresAction (int gameId, boolean single) {
            super(gameId);
            _single = single;
        }

        public ResetScoresAction () {
        }

        @Override // from PeerManager.NodeAction
        protected void execute () {
            _gameReg.resetGameScores(_gameId, _single);
        }

        protected boolean _single;
        @Inject protected transient WorldGameRegistry _gameReg;
    }

    // our dependencies
    @Inject protected MemberManager _memberMan;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected TrophyRepository _trophyRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected TrophySourceRepository _trophySourceRepo;

    /** Comparator for sorting {@link GameInfo}, by gameId. */
    protected static Comparator<GameInfo> SORT_BY_NEWEST = new Comparator<GameInfo>() {
        public int compare (GameInfo c1, GameInfo c2) {
            return c2.gameId - c1.gameId;
        }
    };

    /** Comparator for sorting {@link GameInfo}, by name. */
    protected static Comparator<GameInfo> SORT_BY_NAME = new Comparator<GameInfo>() {
        public int compare (GameInfo c1, GameInfo c2) {
            return c1.name.toString().toLowerCase().compareTo(c2.name.toString().toLowerCase());
        }
    };

    /** Compartor for sorting {@link GameInfo}, with multiplayer games first. */
    protected static Comparator<GameInfo> SORT_BY_MULTIPLAYER = new Comparator<GameInfo>() {
        public int compare (GameInfo c1, GameInfo c2) {
            if (c1.maxPlayers == 1 && c2.maxPlayers > 1) {
                return 1;
            }
            else if (c1.maxPlayers > 1 && c2.maxPlayers == 1) {
                return -1;
            }
            return 0;
        }
    };

    /** Compartor for sorting {@link GameInfo}, with single player games first. */
    protected static Comparator<GameInfo> SORT_BY_SINGLE_PLAYER = new Comparator<GameInfo>() {
        public int compare (GameInfo c1, GameInfo c2) {
            if (c1.minPlayers == 1 && c2.minPlayers > 1) {
                return -1;
            }
            else if (c1.minPlayers > 1 && c2.minPlayers == 1) {
                return 1;
            }
            return 0;
        }
    };

    /** Compartor for sorting {@link GameInfo}, by genre. */
    protected static Comparator<GameInfo> SORT_BY_GENRE = new Comparator<GameInfo>() {
        public int compare (GameInfo c1, GameInfo c2) {
            return c2.genre - c1.genre;
        }
    };

    /** Compartor for sorting {@link GameInfo}, by # of players online. */
    protected static Comparator<GameInfo> SORT_BY_PLAYERS_ONLINE = new Comparator<GameInfo>() {
        public int compare (GameInfo c1, GameInfo c2) {
            return c2.playersOnline - c1.playersOnline;
        }
    };

    protected static final int MAX_RANKINGS = 10;
    protected static final int ARCADE_RAW_COUNT = 200;

    /** Players that haven't played a rated game in 14 days are not included in top-ranked. */
    protected static final long RATING_CUTOFF = 14 * 24*60*60*1000L;
}
