//
// $Id$

package com.threerings.msoy.game.server;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.web.gwt.LaunchConfig;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.FeaturedGameInfo;
import com.threerings.msoy.game.server.WorldGameRegistry;
import com.threerings.msoy.game.server.persist.GameDetailRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
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
    public LaunchConfig loadLaunchConfig (int gameId)
        throws ServiceException
    {
        // load up the metadata for this game
        GameRecord grec = _mgameRepo.loadGameRecord(gameId);
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
            GameDetailRecord detail = _mgameRepo.loadGameDetail(card.placeId);
            GameRecord game = _mgameRepo.loadGameRecord(card.placeId, detail);
            if (game != null && game.getRating() >= 4 && detail.gamesPlayed > 0) {
                featured.add(toFeaturedGameInfo(game, detail, card.population));
                have.add(game.gameId);
            }
            if (have.size() == ArcadeData.FEATURED_GAME_COUNT) {
                break;
            }
        }

        // pad the featured games with ones nobody is playing
        if (featured.size() < ArcadeData.FEATURED_GAME_COUNT) {
            for (GameRecord game : _gameRepo.loadGenre((byte)-1, ArcadeData.FEATURED_GAME_COUNT)) {
                if (!have.contains(game.gameId) && game.getRating() >= 4) {
                    GameDetailRecord detail = _mgameRepo.loadGameDetail(game.gameId);
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

    /**
     * Returns the node name of the world server that is hosting the game being played by the
     * specified player. Returns null if the player is not online or not playing a game.
     */
    public String getPlayerWorldGameNode (final int memberId)
    {
        // we want to avoid looking up their current coin count if we can, so first we pop over to
        // the dobjmgr thread to see if they're currently in a game
        FutureTask<String> findHost = new FutureTask<String>(new Callable<String>() {
            public String call () {
                MemberLocation memloc = _peerMan.getMemberLocation(memberId);
                if (memloc != null && memloc.gameId != 0) {
                    Tuple<String, HostedGame> gameHost = _peerMan.getGameHost(memloc.gameId);
                    if (gameHost != null) {
                        return gameHost.left;
                    }
                }
                return null;
            }
        });
        _omgr.postRunnable(findHost);

        try {
            return findHost.get();
        } catch (Exception e) {
            log.warning("getPlayerWorldGameNode lookup failure", "memberId", memberId, e);
            return null; // nothing to do here but NOOP
        }
    }

    /**
     * If the specified member is identified as being in a game and if their current in-database
     * coin balance is less than the specified number of required coins, sends a notification to
     * the server hosting their game to flush any pending earnings they may have if they have
     * enough pending earnings to afford the coin cost specified.
     */
    public void maybeFlushCoinEarnings (int memberId, int requiredCoins)
    {
        // we'll either get null meaning they're not playing a game or the world node that is
        // hosting the game that they're playing
        String gameHost = getPlayerWorldGameNode(memberId);
        if (gameHost == null) {
            return; // they're not playing, no problem!
        }

        // they're in a game, so let's go ahead and load up their bankbook and see if they can
        // already afford the item in question
        MemberMoney money = _moneyLogic.getMoneyFor(memberId);
        if (money == null) {
            log.warning("maybeFlushCoinEarnings can't find money for member", "memberId", memberId);
            return;
        }
        if (money.coins >= requiredCoins) {
            return; // they've got plenty of money, no problem!
        }

        // now we know they can't afford whatever they're looking at and they're playing a game, so
        // we have to go ahead and ship an action off to the game's world host and have it tell its
        // game server to flush this member's pending coin earnings
        _peerMan.invokeNodeAction(new FlushCoinsAction(gameHost, memberId));
    }

    protected static class GameLocationWaiter extends ServletWaiter<Tuple<String,Integer>>
        implements WorldGameService.LocationListener
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

    /** Helper action for {@link #maybeFlushCoinEarnings}. */
    protected static class FlushCoinsAction extends MsoyPeerManager.NodeAction
    {
        public FlushCoinsAction (String nodeName, int memberId) {
            _nodeName = nodeName;
            _memberId = memberId;
        }
        public FlushCoinsAction () {
        }
        public boolean isApplicable (NodeObject nodeobj) {
            return nodeobj.nodeName.equals(_nodeName);
        }
        protected void execute () {
            _gameReg.flushCoinEarnings(_memberId);
        }
        protected String _nodeName;
        protected int _memberId;
        @Inject protected transient GameGameRegistry _gameReg;
    }

    @Inject protected RootDObjectManager _omgr;
    @Inject protected WorldGameRegistry _gameReg;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected MemberRepository _memberRepo;
}
