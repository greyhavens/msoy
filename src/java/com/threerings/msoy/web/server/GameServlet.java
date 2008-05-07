//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.server.persist.GameDetailRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.peer.server.GameNodeAction;
import com.threerings.msoy.person.server.persist.ProfileRecord;

import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.game.xml.MsoyGameParser;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.HTMLSanitizer;

import com.threerings.msoy.web.client.GameService;
import com.threerings.msoy.web.data.ArcadeData;
import com.threerings.msoy.web.data.FeaturedGameInfo;
import com.threerings.msoy.web.data.GameDetail;
import com.threerings.msoy.web.data.GameGenreData;
import com.threerings.msoy.web.data.GameInfo;
import com.threerings.msoy.web.data.GameMetrics;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.PlaceCard;
import com.threerings.msoy.web.data.PlayerRating;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TrophyCase;
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
        MemberRecord mrec = getAuthedUser(ident);

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

            PlaceCard game = MsoyServer.memberMan.getPPSnapshot().getGame(gameId);
            if (game != null) {
                detail.playingNow = game.population;
            }

            // determine how many players can play this game
            int[] players = getMinMaxPlayers(Game.isDeveloperVersion(gameId) ?
                                             detail.sourceItem : detail.listedItem);
            detail.minPlayers = players[0];
            detail.maxPlayers = players[1];

            return detail;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load game detail [id=" + gameId + "].", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public GameMetrics loadGameMetrics (WebIdent ident, int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);
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
            log.log(Level.WARNING, "Failed to load game metrics [id=" + gameId + "].", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public void updateGameInstructions (WebIdent ident, int gameId, String instructions)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);
        requireIsGameOwner(gameId, mrec);

        try {
            // trust not the user; they are prone to evil
            instructions = HTMLSanitizer.sanitize(instructions);

            // now that we've confirmed that they're allowed, update the instructions
            _gameRepo.updateGameInstructions(gameId, instructions);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update instructions [for=" + mrec.who() +
                    ", gameId=" + gameId + "].", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public void resetGameScores (WebIdent ident, int gameId, boolean single)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);
        requireIsGameOwner(gameId, mrec);

        try {
            // wipe the percentiler in the database
            int uGameId = single ? -gameId : gameId;
            _ratingRepo.updatePercentile(uGameId, new Percentiler());

            // tell any resolved instance of this game to clear its in memory percentiler
            MsoyServer.peerMan.invokeNodeAction(new ResetScoresAction(gameId, single));

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update instructions [for=" + mrec.who() +
                    ", gameId=" + gameId + "].", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public List loadGameTrophies (WebIdent ident, int gameId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        // we only provide trophies for listed games
        if (gameId < 0) {
            log.warning("Requested trophy info for non-listed game [id=" + gameId + "].");
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }

        try {
            GameRecord grec = _gameRepo.loadGameRecord(gameId);
            if (grec == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }
            return loadTrophyInfo(grec, (mrec == null) ? 0 : mrec.memberId, null, null);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure loading game trophies [id=" + gameId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Compares the trophy earnings for the specified set of members in the specified game.
     */
    public CompareResult compareTrophies (WebIdent ident, int gameId, int[] memberIds)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);
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
            log.log(Level.WARNING, "Failure comparing game trophies [id=" + gameId +
                    ", mids=" + StringUtil.toString(memberIds) + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public TrophyCase loadTrophyCase (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

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
            log.log(Level.WARNING, "Failure loading trophies [tgtid=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public PlayerRating[][] loadTopRanked (WebIdent ident, int gameId, boolean onlyMyFriends)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);
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
            for (MemberName name : _memberRepo.loadMemberNames(memIds)) {
                PlayerRating pr = players.get(name.getMemberId());
                pr.name = name;
            }

            // resolve their profile photos
            for (ProfileRecord profile : MsoyServer.profileRepo.loadProfiles(memIds)) {
                PlayerRating pr = players.get(profile.memberId);
                pr.photo = profile.getPhoto();
            }

            // create our result arrays and fill in their actual ratings
            return new PlayerRating[][] { toRatingResult(single, players),
                                          toRatingResult(multi, players) };

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure loading rankings [for=" + ident + ", gameId=" + gameId +
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
            PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();

            // determine the "featured" games
            List<FeaturedGameInfo> featured = Lists.newArrayList();
            ArrayIntSet have = new ArrayIntSet();
            for (PlaceCard card : pps.getTopGames()) {
                GameDetailRecord detail = _gameRepo.loadGameDetail(card.placeId);
                GameRecord game = _gameRepo.loadGameRecord(card.placeId, detail);
                if (game != null) {
                    featured.add(toFeaturedGameInfo(game, detail, card.population));
                    have.add(game.gameId);
                }
            }
            if (featured.size() < ArcadeData.FEATURED_GAME_COUNT) {
                for (GameRecord game :
                         _gameRepo.loadGenre((byte)-1, ArcadeData.FEATURED_GAME_COUNT)) {
                    if (!have.contains(game.gameId)) {
                        GameDetailRecord detail = _gameRepo.loadGameDetail(game.gameId);
                        featured.add(toFeaturedGameInfo(game, detail, 0));
                    }
                    if (featured.size() == ArcadeData.FEATURED_GAME_COUNT) {
                        break;
                    }
                }
            }
            data.featuredGames = featured.toArray(new FeaturedGameInfo[featured.size()]);

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

                // we want to show N games, take the top 3*N and shuffle them
                games = games.subList(
                    0, Math.min(games.size(), 3*ArcadeData.Genre.HIGHLIGHTED_GAMES));
                Collections.shuffle(games);

                // then take N from that shuffled list as the games to show
                games = games.subList(0, Math.min(games.size(), ArcadeData.Genre.HIGHLIGHTED_GAMES));
                genre.games = new GameInfo[games.size()];
                for (int ii = 0; ii < genre.games.length; ii++) {
                    genre.games[ii] = games.get(ii).toGameInfo();
                    PlaceCard ppg = pps.getGame(genre.games[ii].gameId);
                    if (ppg != null) {
                        genre.games[ii].playersOnline = ppg.population;
                    }
                }
                genres.add(genre);
            }
            data.genres = genres;

            // TODO: load mrec and favorite games

            return data;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "loadArcadeData failed [for=" + ident + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public GameGenreData loadGameGenre (WebIdent ident, byte genre)
        throws ServiceException
    {
        try {
            GameGenreData data = new GameGenreData();

            // load up all the games in this genre
            List<GameRecord> games = _gameRepo.loadGenre(genre, -1);

            // convert them to game info objects
            List<GameInfo> infos = Lists.newArrayList();
            for (GameRecord grec : games) {
                infos.add(grec.toGameInfo());
            }
            data.games = infos;

            // determine the "featured" games
            PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();
            List<FeaturedGameInfo> featured = Lists.newArrayList();
            for (int ii = 0; ii < Math.min(games.size(), ArcadeData.FEATURED_GAME_COUNT); ii++) {
                GameRecord game = games.get(ii);
                GameDetailRecord detail = _gameRepo.loadGameDetail(game.gameId);
                PlaceCard card = pps.getGame(game.gameId);
                featured.add(toFeaturedGameInfo(game, detail, card == null ? 0 : card.population));
            }
            data.featuredGames = featured.toArray(new FeaturedGameInfo[featured.size()]);

            return data;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "loadGameGenre failed [for=" + ident + ", genre=" + genre + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected int[] getMinMaxPlayers (Game game)
    {
        MsoyMatchConfig match = null;
        try {
            if (game != null && !StringUtil.isBlank(game.config)) {
                match = (MsoyMatchConfig)new MsoyGameParser().parseGame(game).match;
            }
            if (match == null) {
                log.warning("Game missing match configuration [game=" + game + "].");
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to parse XML game definition [id=" + game.gameId +
                    ", config=" + game.config + "]", e);
        }
        if (match != null) {
            return new int[] {
                match.minSeats,
                (match.getMatchType() == GameConfig.PARTY) ? Integer.MAX_VALUE : match.maxSeats
            };
        }
        return new int[] { 1, 2 }; // arbitrary defaults
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

    protected FeaturedGameInfo toFeaturedGameInfo (GameRecord game, GameDetailRecord detail, int pop)
        throws PersistenceException
    {
        FeaturedGameInfo info = (FeaturedGameInfo)game.toGameInfo(new FeaturedGameInfo());
        info.avgDuration = detail.toGameDetail().getAverageDuration();
        int[] players = getMinMaxPlayers((Game)game.toItem());
        info.minPlayers = players[0];
        info.maxPlayers = players[1];
        info.playersOnline = pop;
        info.creator = _memberRepo.loadMemberName(game.creatorId);
        return info;
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
            log.log(Level.WARNING, "Failed to load game source record to verify ownership " +
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
        TrophySourceRepository tsrepo = MsoyServer.itemMan.getTrophySourceRepository();

        // the negative catalog id is the id for listed items in a game's suite
        int gameSuiteId = -grec.catalogId;

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
            MsoyServer.gameReg.resetGameScores(_gameId, _single);
        }

        protected boolean _single;
    }

    protected MemberRepository _memberRepo = MsoyServer.memberRepo;
    protected GameRepository _gameRepo = MsoyServer.itemMan.getGameRepository();
    protected TrophyRepository _trophyRepo = MsoyServer.trophyRepo;
    protected RatingRepository _ratingRepo = MsoyServer.ratingRepo;

    protected static final int MAX_RANKINGS = 10;
}
