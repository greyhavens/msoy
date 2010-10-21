//
// $Id$

package com.threerings.msoy.game.server;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.web.gwt.ServiceException;
import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.all.LaunchConfig;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.edgame.gwt.GameCode;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.web.server.ServletWaiter;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
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
        GameInfoRecord game = _mgameRepo.loadGame(gameId);
        GameCode code = _mgameRepo.loadGameCode(gameId, false);
        if (game == null || code == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // create a launch config record for the game
        final LaunchConfig config = new LaunchConfig();
        config.gameId = gameId;

        MsoyMatchConfig match;
        boolean roomless = false;
        try {
            if (StringUtil.isBlank(code.config)) {
                // fall back to a sensible default for our legacy games
                match = new MsoyMatchConfig();
                match.minSeats = match.startSeats = 1;
                match.maxSeats = 2;
            } else {
                MsoyGameDefinition def = (MsoyGameDefinition)new MsoyGameParser().parseGame(code);
                config.lwjgl = def.lwjgl;
                roomless = def.roomless;
                match = (MsoyMatchConfig)def.match;
            }

        } catch (Exception e) {
            log.warning("Failed to parse XML game definition [id=" + gameId + "]", e);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }

        switch (code.clientMedia.getMimeType()) {
        case MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH:
            config.type = game.isAVRG ?
                LaunchConfig.FLASH_IN_WORLD : LaunchConfig.FLASH_LOBBIED;
            break;
        case MediaMimeTypes.APPLICATION_JAVA_ARCHIVE:
            // ignore maxSeats in the case of a party game - always display a lobby
            config.type = (!match.isPartyGame && match.maxSeats == 1) ?
                LaunchConfig.JAVA_SOLO : LaunchConfig.JAVA_FLASH_LOBBIED;
            break;
        default:
            log.warning("Requested config for game of unknown media type " +
                        "[id=" + gameId + ", media=" + code.clientMedia + "].");
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        // we have to proxy game jar files through the game server due to the applet sandbox
        config.clientMediaPath = (code.clientMedia.getMimeType() == MediaMimeTypes.APPLICATION_JAVA_ARCHIVE) ?
            code.clientMedia.getProxyMediaPath() : code.clientMedia.getMediaPath();
        config.name = game.name;
        config.httpPort = ServerConfig.httpPort;

        // if this is an AVRG (and not roomless), send the user to its group's home scene
        if (game.isAVRG) {
            if (roomless) {
                config.sceneId = 0;

            } else {
                GroupRecord gprec = (game.groupId != 0) ? _groupRepo.loadGroup(game.groupId) : null;
                // (gprec may be null even if we try to load it from the repo).
                // Play in group's home, or user's home.
                config.sceneId = (gprec != null) ? gprec.homeSceneId : Integer.MIN_VALUE;
            }
        }

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
     * If the specified member is identified as being in a game and if their current in-database
     * coin balance is less than the specified number of required coins, sends a notification to
     * the server hosting their game to flush any pending earnings they may have if they have
     * enough pending earnings to afford the coin cost specified.
     */
    public void maybeFlushCoinEarnings (final int memberId, int requiredCoins)
    {
        // we want to avoid looking up their current coin count if we can, so first we pop over to
        // the dobjmgr thread to see if they're currently in a game
        FutureTask<Boolean> findClient = new FutureTask<Boolean>(new Callable<Boolean>() {
            public Boolean call () {
                return _peerMan.locateClient(GameAuthName.makeKey(memberId)) != null;
            }
        });
        _omgr.postRunnable(findClient);

        try {
            if (!findClient.get()) {
                return; // not in a game, no need to continue
            }
        } catch (Exception e) {
            log.warning("Failed to locate player's game client", "memberId", memberId, e);
            return; // nothing to do here but NOOP
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
        _playerActions.flushCoins(memberId);
    }

    protected static boolean canFeature (GameInfoRecord grec)
    {
        return grec.integrated && grec.getRating() >= MIN_FEATURED_RATING;
    }

    protected static class GameLocationWaiter extends ServletWaiter<Tuple<String,Integer>>
        implements WorldGameService.LocationListener
    {
        public int gameId;
        public GameLocationWaiter (int gameId) {
            super("locateGame(" + gameId + ")");
            this.gameId = gameId;
        }
        public void gameLocated (String host, int port, boolean isAVRG) {
            postSuccess(new Tuple<String,Integer>(host, port));
        }
        public void requestFailed (String cause) {
            requestFailed(new InvocationException(cause));
        }
    }

    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected PlayerNodeActions _playerActions;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected WorldGameRegistry _gameReg;

    protected static final byte MIN_FEATURED_RATING = 4;
}
