//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.xml.MsoyGameParser;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.server.persist.GameDetailRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.RatingRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.GameService;
import com.threerings.msoy.web.data.GameDetail;
import com.threerings.msoy.web.data.LaunchConfig;
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
    public LaunchConfig loadLaunchConfig (WebIdent ident, int gameId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        // load up the metadata for this game
        GameRepository repo = MsoyServer.itemMan.getGameRepository();
        GameRecord grec;
        try {
            grec = repo.loadGameRecord(gameId);
            if (grec == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load game record [gameId=" + gameId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
        final Game game = (Game)grec.toItem();

        // create a launch config record for the game
        LaunchConfig config = new LaunchConfig();
        config.gameId = game.gameId;

        MsoyMatchConfig match;
        try {
            if (StringUtil.isBlank(game.config)) {
                // fall back to a sensible default for our legacy games
                match = new MsoyMatchConfig();
                match.minSeats = match.startSeats = 1;
                match.maxSeats = 2;
            } else {
                MsoyGameDefinition def = (MsoyGameDefinition)new MsoyGameParser().parseGame(game);
                config.lwjgl = def.lwjgl;
                match = (MsoyMatchConfig)def.match;
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to parse XML game definition [id=" + gameId + "]", e);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }

        switch (game.gameMedia.mimeType) {
        case MediaDesc.APPLICATION_SHOCKWAVE_FLASH:
            config.type = game.isInWorld() ?
                    LaunchConfig.FLASH_IN_WORLD : LaunchConfig.FLASH_LOBBIED;
            break;
        case MediaDesc.APPLICATION_JAVA_ARCHIVE:
            // ignore maxSeats in the case of a party game - always display a lobby
            config.type = (!match.isPartyGame && match.maxSeats == 1) ?
                LaunchConfig.JAVA_SOLO : LaunchConfig.JAVA_FLASH_LOBBIED;
            break;
        default:
            log.warning("Requested config for game of unknown media type " +
                        "[id=" + gameId + ", media=" + game.gameMedia + "].");
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        // we have to proxy game jar files through the game server due to the applet sandbox
        config.gameMediaPath = (game.gameMedia.mimeType == MediaDesc.APPLICATION_JAVA_ARCHIVE) ?
            game.gameMedia.getProxyMediaPath() : game.gameMedia.getMediaPath();
        config.name = game.name;
        config.httpPort = ServerConfig.httpPort;

        // determine what server is hosting the game, if any
        Tuple<String, Integer> rhost = MsoyServer.peerMan.getGameHost(gameId);
        if (rhost != null) {
            config.server = MsoyServer.peerMan.getPeerPublicHostName(rhost.left);
            config.port = rhost.right;
        }

        return config;
    }

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
                    RatingRecord<GameRecord> rr = repo.getRating(item.itemId, mrec.memberId);
                    detail.memberRating = (rr == null) ? 0 : rr.rating;
                }
            }

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
    public Trophy[] loadGameTrophies (WebIdent ident, int gameId)
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

            HashMap<String,Trophy> trophies = new HashMap<String,Trophy>();
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
            Trophy[] result = new Trophy[trecords.size()];
            for (int ii = 0; ii < result.length; ii++) {
                TrophySourceRecord record = trecords.get(ii);
                result[ii] = trophies.get(record.ident);
                // only provide the description for non-secret or earned trophies
                if (!record.secret || result[ii].whenEarned != null) {
                    result[ii].description = record.description;
                }
            }
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure loading game trophies [id=" + gameId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
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

            HashIntMap<ArrayList<Trophy>> tmap = new HashIntMap<ArrayList<Trophy>>();
            for (TrophyRecord trec : MsoyServer.trophyRepo.loadTrophies(memberId)) {
                ArrayList<Trophy> tlist = tmap.get(trec.gameId);
                if (tlist == null) {
                    tmap.put(trec.gameId, tlist = new ArrayList<Trophy>());
                }
                tlist.add(trec.toTrophy());
            }

            tcase.shelves = new TrophyCase.Shelf[tmap.size()];
            int ii = 0;
            for (IntMap.IntEntry<ArrayList<Trophy>> entry : tmap.intEntrySet()) {
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
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }
}
