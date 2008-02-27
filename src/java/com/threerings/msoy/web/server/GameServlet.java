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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.rating.server.persist.RatingRecord;
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

import com.threerings.msoy.person.server.persist.ProfileRecord;

import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.xml.MsoyGameParser;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.HTMLSanitizer;

import com.threerings.msoy.web.client.GameService;
import com.threerings.msoy.web.data.ArcadeData;
import com.threerings.msoy.web.data.FeaturedGameInfo;
import com.threerings.msoy.web.data.GameDetail;
import com.threerings.msoy.web.data.GameGenreData;
import com.threerings.msoy.web.data.GameInfo;
import com.threerings.msoy.web.data.GameMetrics;
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
        GameRepository repo = MsoyServer.itemMan.getGameRepository();

        try {
            GameDetailRecord gdr = repo.loadGameDetail(gameId);
            if (gdr == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            GameDetail detail = gdr.toGameDetail();
            int creatorId = 0;
            if (gdr.sourceItemId != 0) {
                ItemRecord item = repo.loadItem(gdr.sourceItemId);
                if (item != null) {
                    detail.sourceItem = (Game)item.toItem();
                    creatorId = item.creatorId;
                }
            }

            if (gdr.listedItemId != 0) {
                ItemRecord item = repo.loadItem(gdr.listedItemId);
                if (item != null) {
                    detail.listedItem = (Game)item.toItem();
                    creatorId = item.creatorId;
                }
                if (mrec != null) {
                    detail.memberRating = repo.getRating(item.itemId, mrec.memberId);
                }
            }

            // determine how many players can play this game
            int[] players = getMinMaxPlayers(detail.getGame());
            detail.minPlayers = players[0];
            detail.maxPlayers = players[1];

            if (creatorId != 0) {
                MemberRecord crrec = MsoyServer.memberRepo.loadMember(creatorId);
                if (crrec == null) {
                    log.warning("Game missing creator " + gdr + ".");
                } else {
                    detail.creator = crrec.getName();
                }
            }

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

        GameRepository repo = MsoyServer.itemMan.getGameRepository();
        try {
            GameMetrics metrics = new GameMetrics();
            metrics.gameId = gameId;

            Percentiler stiler = MsoyServer.ratingRepo.loadPercentile(-gameId);
            if (stiler != null) {
                metrics.singleTotalCount = stiler.getRecordedCount();
                metrics.singleCounts = stiler.getCounts();
                metrics.singleScores = stiler.getRequiredScores();
                metrics.singleMaxScore = stiler.getMaxScore();
            }

            Percentiler mtiler = MsoyServer.ratingRepo.loadPercentile(gameId);
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

        GameRepository repo = MsoyServer.itemMan.getGameRepository();
        try {
            // trust not the user; they are prone to evil
            instructions = HTMLSanitizer.sanitize(instructions);

            // now that we've confirmed that they're allowed, update the instructions
            repo.updateGameInstructions(gameId, instructions);

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

        GameRepository repo = MsoyServer.itemMan.getGameRepository();
        try {
            int uGameId = single ? -gameId : gameId;
            MsoyServer.ratingRepo.updatePercentile(uGameId, new Percentiler());

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
        GameRepository grepo = MsoyServer.itemMan.getGameRepository();
        TrophySourceRepository tsrepo = MsoyServer.itemMan.getTrophySourceRepository();

        // we only provide trophies for listed games
        if (gameId < 0) {
            log.warning("Requested trophies for non-listed game [id=" + gameId + "].");
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }

        try {
            GameRecord grec = grepo.loadGameRecord(gameId);
            if (grec == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }

            Map<String,Trophy> trophies = Maps.newHashMap();
            List<TrophySourceRecord> trecords = tsrepo.loadOriginalItemsBySuite(-grec.catalogId);
            for (TrophySourceRecord record : trecords) {
                Trophy trophy = new Trophy();
                trophy.gameId = gameId;
                trophy.name = record.name;
                trophy.trophyMedia = new MediaDesc(record.thumbMediaHash, record.thumbMimeType);
                trophies.put(record.ident, trophy);
            }

            // fill in earned dates if the caller is authenticated
            if (mrec != null) {
                for (TrophyRecord record :
                         MsoyServer.trophyRepo.loadTrophies(gameId, mrec.memberId)) {
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

            // and populate the result array in the correct order
            List<Trophy> results = Lists.newArrayList();
            for (TrophySourceRecord record : trecords) {
                Trophy trophy = trophies.get(record.ident);
                // only provide the description for non-secret or earned trophies
                if (!record.secret || trophy.whenEarned != null) {
                    trophy.description = record.description;
                }
                results.add(trophy);
            }
            return results;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure loading game trophies [id=" + gameId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GameService
    public TrophyCase loadTrophyCase (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            MemberRecord tgtrec = MsoyServer.memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            TrophyCase tcase = new TrophyCase();
            tcase.owner= tgtrec.getName();

            IntMap<List<Trophy>> tmap = IntMaps.newHashIntMap();
            for (TrophyRecord trec : MsoyServer.trophyRepo.loadTrophies(memberId)) {
                List<Trophy> tlist = tmap.get(trec.gameId);
                if (tlist == null) {
                    tmap.put(trec.gameId, tlist = Lists.newArrayList());
                }
                tlist.add(trec.toTrophy());
            }

            tcase.shelves = new TrophyCase.Shelf[tmap.size()];
            int ii = 0;
            for (IntMap.IntEntry<List<Trophy>> entry : tmap.intEntrySet()) {
                TrophyCase.Shelf shelf = new TrophyCase.Shelf();
                shelf.gameId = entry.getIntKey();
                shelf.trophies = entry.getValue().toArray(new Trophy[entry.getValue().size()]);
                Arrays.sort(shelf.trophies, new Comparator<Trophy>() {
                    public int compare (Trophy t1, Trophy t2) {
                        return t2.whenEarned.compareTo(t1.whenEarned);
                    }
                });
                GameRecord grec = MsoyServer.itemMan.getGameRepository().loadGameRecord(
                    shelf.gameId);
                if (grec == null) {
                    log.warning("Have trophies for unknown game [who=" + memberId +
                                ", gameId=" + shelf.gameId + "].");
                    shelf.name = "???";
                } else {
                    shelf.name = grec.name;
                }
                tcase.shelves[ii++] = shelf;
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
                friendIds = MsoyServer.memberRepo.loadFriendIds(mrec.memberId);
                friendIds.add(mrec.memberId); // us too!
            }

            // load up the single and mutiplayer ratings
            List<RatingRecord> single = MsoyServer.ratingRepo.getTopRatings(
                -gameId, MAX_RANKINGS, friendIds);
            List<RatingRecord> multi = MsoyServer.ratingRepo.getTopRatings(
                gameId, MAX_RANKINGS, friendIds);

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
            for (MemberName name : MsoyServer.memberRepo.loadMemberNames(memIds)) {
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
            GameRepository grepo = MsoyServer.itemMan.getGameRepository();
            PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();

            // determine the "featured" games
            List<FeaturedGameInfo> featured = Lists.newArrayList();
            for (PlaceCard card : pps.getTopGames()) {
                GameDetailRecord detail = grepo.loadGameDetail(card.placeId);
                GameRecord game = grepo.loadGameRecord(card.placeId, detail);
                if (game != null) {
                    featured.add(toFeaturedGameInfo(game, detail, card.population));
                }
            }
            int need = ArcadeData.FEATURED_GAME_COUNT - featured.size();
            for (GameRecord game : grepo.loadGenre((byte)-1, need)) {
                GameDetailRecord detail = grepo.loadGameDetail(game.gameId);
                featured.add(toFeaturedGameInfo(game, detail, 0));
            }
            data.featuredGames = featured.toArray(new FeaturedGameInfo[featured.size()]);

            // load information about the genres
            List<ArcadeData.Genre> genres = Lists.newArrayList();
            for (byte gcode : Game.GENRES) {
                ArcadeData.Genre genre = new ArcadeData.Genre();
                genre.genre = gcode;
                List<GameRecord> games = grepo.loadGenre(gcode, -1);
                genre.gameCount = games.size();
                if (genre.gameCount == 0) {
                    continue;
                }
                int fcount = Math.min(genre.gameCount, ArcadeData.Genre.HIGHLIGHTED_GAMES);
                genre.games = new GameInfo[fcount];
                for (int ii = 0; ii < fcount; ii++) {
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
            GameRepository grepo = MsoyServer.itemMan.getGameRepository();

            // load up all the games in this genre
            List<GameRecord> games = grepo.loadGenre(genre, -1);

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
                GameDetailRecord detail = grepo.loadGameDetail(game.gameId);
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
        info.creator = MsoyServer.memberRepo.loadMemberName(game.creatorId);
        return info;
    }

    protected void requireIsGameOwner (int gameId, MemberRecord mrec)
        throws ServiceException
    {
        GameRepository repo = MsoyServer.itemMan.getGameRepository();
        try {
            // load the source record
            GameRecord grec = repo.loadGameRecord(-gameId);
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

    protected static final int MAX_RANKINGS = 10;
}
