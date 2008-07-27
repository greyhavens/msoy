//
// $Id$

package com.threerings.msoy.web.server;

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

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.MediaDesc;
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
import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.game.server.GameUtil;
import com.threerings.msoy.game.server.MsoyGameRegistry;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.HTMLSanitizer;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.PlaceCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link GameService}.
 */
public class GameServlet extends MsoyServiceServlet
    implements GameService
{
    // from interface GameService
    public GameDetail loadGameDetail (WebIdent ident, int gameId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
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
            if (creatorId != 0) {
                detail.creator = _memberRepo.loadMemberName(creatorId);
            }
            detail.instructions = _gameRepo.loadInstructions(gdr.gameId);

            if (gdr.listedItemId != 0) {
                ItemRecord item = _gameRepo.loadItem(gdr.listedItemId);
                if (item != null) {
                    detail.listedItem = (Game)item.toItem();
                    creatorId = item.creatorId;
                }
                if (mrec != null) {
                    detail.memberRating = _gameRepo.getRating(item.itemId, mrec.memberId);
                }
            }

            PlaceCard game = _memberMan.getPPSnapshot().getGame(gameId);
            if (game != null) {
                detail.playingNow = game.population;
            }

            // determine how many players can play this game
            int[] players = GameUtil.getMinMaxPlayers(Game.isDeveloperVersion(gameId) ?
                                                      detail.sourceItem : detail.listedItem);
            detail.minPlayers = players[0];
            detail.maxPlayers = players[1];

            return detail;

        } catch (PersistenceException pe) {
            log.warning("Failed to load game detail [id=" + gameId + "].", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public GameMetrics loadGameMetrics (WebIdent ident, int gameId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        requireIsGameOwner(gameId, mrec);

        try {
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

        } catch (PersistenceException pe) {
            log.warning("Failed to load game metrics [id=" + gameId + "].", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public GameLogs loadGameLogs (WebIdent ident, int gameId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        requireIsGameOwner(gameId, mrec);

        try {
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

        } catch (PersistenceException pe) {
            log.warning("Failed to load game logs [id=" + gameId + "].", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public void updateGameInstructions (WebIdent ident, int gameId, String instructions)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        requireIsGameOwner(gameId, mrec);

        try {
            // trust not the user; they are prone to evil
            instructions = HTMLSanitizer.sanitize(instructions);

            // now that we've confirmed that they're allowed, update the instructions
            _gameRepo.updateInstructions(gameId, instructions);

        } catch (PersistenceException pe) {
            log.warning("Failed to update instructions [for=" + mrec.who() +
                    ", gameId=" + gameId + "].", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public void resetGameScores (WebIdent ident, int gameId, boolean single)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        requireIsGameOwner(gameId, mrec);

        try {
            // wipe the percentiler in the database
            int uGameId = single ? -gameId : gameId;
            _ratingRepo.updatePercentile(uGameId, new Percentiler());

            // tell any resolved instance of this game to clear its in memory percentiler
            _peerMan.invokeNodeAction(new ResetScoresAction(gameId, single));

        } catch (PersistenceException pe) {
            log.warning("Failed to update instructions [for=" + mrec.who() +
                    ", gameId=" + gameId + "].", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public List<Trophy> loadGameTrophies (WebIdent ident, int gameId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            GameRecord grec = _gameRepo.loadGameRecord(gameId);
            if (grec == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }
            return loadTrophyInfo(grec, (mrec == null) ? 0 : mrec.memberId, null, null);

        } catch (PersistenceException pe) {
            log.warning("Failure loading game trophies [id=" + gameId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public CompareResult compareTrophies (WebIdent ident, int gameId, int[] memberIds)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);
        int callerId = (mrec == null) ? 0 : mrec.memberId;

        try {
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

        } catch (PersistenceException pe) {
            log.warning("Failure comparing game trophies [id=" + gameId +
                    ", mids=" + StringUtil.toString(memberIds) + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public TrophyCase loadTrophyCase (WebIdent ident, int memberId)
        throws ServiceException
    {
        try {
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

        } catch (PersistenceException pe) {
            log.warning("Failure loading trophies [tgtid=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public PlayerRating[][] loadTopRanked (WebIdent ident, int gameId, boolean onlyMyFriends)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);
        if (mrec == null && onlyMyFriends) {
            log.warning("Requested friend rankings for non-authed member [gameId=" + gameId + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        try {
            // if we should restrict to just this player's friends, figure out who those are
            IntSet friendIds = null;
            if (onlyMyFriends) {
                friendIds = _memberRepo.loadFriendIds(mrec.memberId);
                friendIds.add(mrec.memberId); // us too!
            }

            // load up the single and mutiplayer ratings
            List<RatingRecord> single = _ratingRepo.getTopRatings(-gameId, MAX_RANKINGS, friendIds);
            List<RatingRecord> multi = _ratingRepo.getTopRatings(gameId, MAX_RANKINGS, friendIds);

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

        } catch (PersistenceException pe) {
            log.warning("Failure loading rankings [for=" + ident + ", gameId=" + gameId +
                    ", friends=" + onlyMyFriends + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public ArcadeData loadArcadeData (WebIdent ident)
        throws ServiceException
    {
        try {
            ArcadeData data = new ArcadeData();
            PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();

            // determine the "featured" games
            List<FeaturedGameInfo> featured = Lists.newArrayList();
            ArrayIntSet have = new ArrayIntSet();
            for (PlaceCard card : pps.getTopGames()) {
                GameDetailRecord detail = _gameRepo.loadGameDetail(card.placeId);
                GameRecord game = _gameRepo.loadGameRecord(card.placeId, detail);
                if (game != null) {
                    featured.add(_gameLogic.toFeaturedGameInfo(game, detail, card.population));
                    have.add(game.gameId);
                }
            }
            if (featured.size() < ArcadeData.FEATURED_GAME_COUNT) {
                for (GameRecord game :
                         _gameRepo.loadGenre((byte)-1, ArcadeData.FEATURED_GAME_COUNT)) {
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

            // list of all games alphabetically (only include name and id)
            data.allGames = Lists.newArrayList();
            for (GameRecord game : _gameRepo.loadGenre((byte)-1, -1)) {
                GameInfo gameInfo = new GameInfo();
                gameInfo.gameId = game.gameId;
                gameInfo.name = game.name;
                data.allGames.add(gameInfo);
            }
            Collections.sort(data.allGames, SORT_BY_NAME);

            // list of top 10 games by ranking (include name, id & media)
            data.topGames = Lists.newArrayList();
            for (GameRecord game : _gameRepo.loadGenre((byte)-1, ArcadeData.TOP_GAME_COUNT)) {
                GameInfo gameInfo = new GameInfo();
                gameInfo.gameId = game.gameId;
                gameInfo.name = game.name;
                gameInfo.thumbMedia = game.getThumbMediaDesc();
                data.topGames.add(gameInfo);
            }

            // load information about the genres
            List<ArcadeData.Genre> genres = Lists.newArrayList();
            for (byte gcode : Game.GENRES) {
                ArcadeData.Genre genre = new ArcadeData.Genre();
                genre.genre = gcode;
                List<GameRecord> games = _gameRepo.loadGenre(gcode, -1);
                genre.gameCount = games.size();
                if (genre.gameCount == 0) {
                    continue;
                }

                // select random N from the top 3N games ranked 3+ after at least 10 votes
                List<GameRecord> goodGames = Lists.newArrayList();
                for (GameRecord game : games) {
                    if (game.rating >= 3 || game.ratingCount < 10) {
                        goodGames.add(game);
                        if (goodGames.size() == 3*ArcadeData.Genre.HIGHLIGHTED_GAMES) {
                            break;
                        }
                    }
                }
                Collections.shuffle(goodGames);

                // then take N from that shuffled list as the games to show
                goodGames = goodGames.subList(
                    0, Math.min(goodGames.size(), ArcadeData.Genre.HIGHLIGHTED_GAMES));
                genre.games = new GameInfo[goodGames.size()];
                for (int ii = 0; ii < genre.games.length; ii++) {
                    genre.games[ii] = goodGames.get(ii).toGameInfo();
                    PlaceCard ppg = pps.getGame(genre.games[ii].gameId);
                    if (ppg != null) {
                        genre.games[ii].playersOnline = ppg.population;
                    }
                }

                genres.add(genre);
            }
            data.genres = genres;

            return data;

        } catch (PersistenceException pe) {
            log.warning("loadArcadeData failed [for=" + ident + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public List<GameInfo> loadGameGenre (WebIdent ident, byte genre, byte sortMethod, String query)
        throws ServiceException
    {
        try {
            PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();

            // load up all the games in this genre
            List<GameRecord> games = _gameRepo.loadGenre(genre, -1, query);

            // convert them to game info objects
            List<GameInfo> infos = Lists.newArrayList();
            for (GameRecord grec : games) {
                GameInfo info = grec.toGameInfo();
                // add the players online
                PlaceCard gameCard = pps.getGame(grec.gameId);
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

        } catch (PersistenceException pe) {
            log.warning("loadGameGenre failed [for=" + ident + ", genre=" + genre + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public FeaturedGameInfo[] loadTopGamesData (WebIdent ident)
        throws ServiceException
    {
        try {
            return _gameLogic.loadTopGames(_memberMan.getPPSnapshot());

        } catch (PersistenceException pe) {
            log.warning("loadTopGamesData failed [for=" + ident + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
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
        try {
            // load the source record
            GameRecord grec = _gameRepo.loadGameRecord(-gameId);
            if (grec == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }
            // verify that the member in question owns the game or is an admin
            if (grec.ownerId != mrec.memberId && !mrec.isAdmin()) {
                throw new ServiceException(InvocationCodes.E_ACCESS_DENIED);
            }

        } catch (PersistenceException pe) {
            log.warning("Failed to load game source record to verify ownership " +
                    "[gameId=" + gameId + ", mrec=" + mrec.who() + "].");
        }
    }

    /**
     * Helper function for {@link #loadGameTrophies} and {@link #compareTrophies}.
     */
    protected List<Trophy> loadTrophyInfo (GameRecord grec, int callerId,
                                           int[] earnerIds, Long[][] whenEarneds)
        throws ServiceException, PersistenceException
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

        @Override // from PeerManager.NodeAction
        protected void execute () {
            _gameReg.resetGameScores(_gameId, _single);
        }

        protected boolean _single;
        @Inject protected transient MsoyGameRegistry _gameReg;
    }

    // our dependencies
    @Inject protected MemberManager _memberMan;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected TrophyRepository _trophyRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected TrophySourceRepository _trophySourceRepo;

    /** Compartor for sorting {@link GameInfo}, by gameId. */
    protected static Comparator<GameInfo> SORT_BY_NEWEST = new Comparator<GameInfo>() {
        public int compare (GameInfo c1, GameInfo c2) {
            return c2.gameId - c1.gameId;
        }
    };

    /** Compartor for sorting {@link GameInfo}, by name. */
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
}
