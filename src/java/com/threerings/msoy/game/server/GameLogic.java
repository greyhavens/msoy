//
// $Id$

package com.threerings.msoy.game.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameDetailRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.FeaturedGameInfo;
import com.threerings.msoy.game.xml.MsoyGameParser;

import static com.threerings.msoy.Log.log;

/**
 * Contains game-related services used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class GameLogic
{
    /**
     * Loads the launch config for the specified game, resolving it on this server if necessary.
     */
    public LaunchConfig loadLaunchConfig (int gameId, boolean assignGuestId)
        throws ServiceException
    {
        // load up the metadata for this game
        GameRecord grec = _gameRepo.loadGameRecord(gameId);
        if (grec == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        Game game = (Game)grec.toItem();

        // create a launch config record for the game
        final LaunchConfig config = new LaunchConfig();
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
            log.warning("Failed to parse XML game definition [id=" + gameId + "]", e);
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

        // determine what server is hosting the game, start hosting it if necessary
        final int gameGroupId = ServerConfig.getGameGroupId(game.groupId);
        final GameLocationWaiter waiter = new GameLocationWaiter(config.gameId);
        _omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    // locate the server that's hosting this game's group whirled and fill that
                    // directly into the LaunchConfig;
                    Tuple<String, HostedRoom> shost = _peerMan.getSceneHost(gameGroupId);
                    if (shost == null) {
                        config.groupServer = ServerConfig.serverHost;
                        config.groupPort = ServerConfig.serverPorts[0];
                    } else {
                        config.groupServer = _peerMan.getPeerPublicHostName(shost.left);
                        config.groupPort = _peerMan.getPeerPort(shost.left);
                    }

                    // now figure out where the game is being hosted; this might result in this
                    // server resolving the game in question, so we have to go through this
                    // listener rigamarole
                    _gameReg.locateGame(null, waiter.gameId, waiter);

                } catch (InvocationException ie) {
                    waiter.requestFailed(ie);
                }
            }
        });
        Tuple<String, Integer> rhost = waiter.waitForResult();
        config.gameServer = rhost.left;
        config.gamePort = rhost.right;

        // finally, if they are a guest and have not yet been assigned a guest id, do so now so
        // that they can log directly into the game server
        if (assignGuestId) {
            config.guestId = _peerMan.getNextGuestId(); // this method is thread safe
        }

        return config;
    }

    /**
     * Loads and returns data on the top games. Used on the landing and arcade pages.
     * Games that do not payout flow, and those with a ranking less than 4 stars not included.
     */
    public FeaturedGameInfo[] loadTopGames (PopularPlacesSnapshot pps)
    {
        // determine the games people are playing right now
        List<FeaturedGameInfo> featured = Lists.newArrayList();
        ArrayIntSet have = new ArrayIntSet();
        for (PopularPlacesSnapshot.Place card : pps.getTopGames()) {
            GameDetailRecord detail = _gameRepo.loadGameDetail(card.placeId);
            GameRecord game = _gameRepo.loadGameRecord(card.placeId, detail);
            if (game != null && game.rating >= 4 && detail.gamesPlayed > 0) {
                featured.add(toFeaturedGameInfo(game, detail, card.population));
                have.add(game.gameId);
            }
        }

        // pad the featured games with ones nobody is playing
        if (featured.size() < ArcadeData.FEATURED_GAME_COUNT) {
            for (GameRecord game : _gameRepo.loadGenre((byte)-1, ArcadeData.FEATURED_GAME_COUNT)) {
                if (!have.contains(game.gameId) && game.rating >= 4) {
                    GameDetailRecord detail = _gameRepo.loadGameDetail(game.gameId);
                    if (detail.gamesPlayed > 0) {
                        featured.add(toFeaturedGameInfo(game, detail, 0));
                    }
                }
                if (featured.size() == ArcadeData.FEATURED_GAME_COUNT) {
                    break;
                }
            }
        }
        return featured.toArray(new FeaturedGameInfo[featured.size()]);
    }

    /**
     * Creates a {@link FeaturedGameInfo} record for the supplied game.
     */
    public FeaturedGameInfo toFeaturedGameInfo (GameRecord game, GameDetailRecord detail, int pop)
    {
        FeaturedGameInfo info = (FeaturedGameInfo)game.toGameInfo(new FeaturedGameInfo());
        info.avgDuration = detail.getAverageDuration();
        int[] players = GameUtil.getMinMaxPlayers((Game)game.toItem());
        info.minPlayers = players[0];
        info.maxPlayers = players[1];
        info.playersOnline = pop;
        info.creator = _memberRepo.loadMemberName(game.creatorId);
        return info;
    }

    protected static class GameLocationWaiter extends ServletWaiter<Tuple<String,Integer>>
        implements MsoyGameService.LocationListener
    {
        public int gameId;
        public GameLocationWaiter (int gameId) {
            super("locateGame(" + gameId + ")");
            this.gameId = gameId;
        }
        public void gameLocated (String host, int port) {
            postSuccess(new Tuple<String,Integer>(host, port));
        }
        public void requestFailed (String cause) {
            requestFailed(new InvocationException(cause));
        }
    }

    @Inject RootDObjectManager _omgr;
    @Inject MsoyGameRegistry _gameReg;
    @Inject MsoyPeerManager _peerMan;
    @Inject GameRepository _gameRepo;
    @Inject MemberRepository _memberRepo;
}
