//
// $Id$

package com.threerings.msoy.game.server;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.net.ConnectionManager;
import com.threerings.presents.peer.data.NodeObject;

import com.threerings.crowd.server.BodyManager;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.game.data.GameCodes;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.AuxSessionFactory;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.notify.server.NotificationManager;

import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.data.GameCredentials;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.TablesWaiting;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.server.RoomManager;

import static com.threerings.msoy.Log.log;

/**
 * Manages the process of starting up external game server processes and coordinating with them as
 * they host lobbies and games.
 */
@Singleton
public class WorldGameRegistry
    implements WorldGameProvider, MsoyPeerManager.PeerObserver
{
    @Inject public WorldGameRegistry (InvocationManager invmgr)
    {
        invmgr.registerDispatcher(new WorldGameDispatcher(this), MsoyCodes.GAME_GROUP);
    }

    /**
     * Initializes this registry and queues up our game servers to be started.
     */
    public void init ()
    {
        // listen for peer connections so that we can manage multiply claimed games
        _peerMan.peerObs.add(this);
    }

    /**
     * Returns true if we're hosting or currently resolving the specified game. It is possible that
     * this will return true during the window where we're finding out that some other node is
     * hosting a game, but we'll just gloss over that because we like the occasional inexplicable
     * behavior and doing the "right thing" is extremely complicated.
     */
    public boolean isHosting (int gameId)
    {
        return _games.contains(gameId) || _penders.containsKey(gameId);
    }

    /**
     * Called during server initialization to give us a chance to wire up our authenticator and
     * session factory.
     */
    public void configSessionFactory (ConnectionManager conmgr, ClientManager clmgr)
    {
        conmgr.addChainedAuthenticator(_injector.getInstance(GameAuthenticator.class));
        clmgr.setSessionFactory(new AuxSessionFactory(
            clmgr.getSessionFactory(), GameCredentials.class, GameAuthName.class,
            GameSession.class, GameClientResolver.class));
    }

    /**
     * Called to update that a player is either lobbying for, playing, or no longer playing the
     * specified game.
     */
    public void updatePlayerGame (final MemberObject memObj, GameSummary game)
    {
        memObj.startTransaction();
        try {
            // check to see if we were previously in an AVRG game
            int avrGameId = (memObj.game != null && memObj.game.avrGame) ? memObj.game.gameId : 0;

            // update our game
            memObj.setGame(game);

            // see if we need to do some extra AVRG bits
            if (avrGameId != 0 || (game != null && game.avrGame)) {
                PlaceManager pmgr = _placeReg.getPlaceManager(memObj.getPlaceOid());
                RoomManager rmgr = (pmgr instanceof RoomManager) ? (RoomManager) pmgr : null;

                // if we left an AVRG, let the room know
                if (rmgr != null && avrGameId != 0 && (game == null || game.gameId != avrGameId)) {
                    rmgr.occupantLeftAVRGame(memObj);
                }

                // if we're now in a new one, subscribe to it
                if (game != null && game.avrGame) {
                    // and immediately let the room manager know
                    if (rmgr != null && game.gameId != avrGameId) {
                        rmgr.occupantEnteredAVRGame(memObj);
                    }
                }
            }

        } finally {
            memObj.commitTransaction();
        }

        // update their occupant info if they're in a scene
        _bodyMan.updateOccupantInfo(memObj, new MemberInfo.Updater<MemberInfo>() {
            public boolean update (MemberInfo info) {
                info.updateGameSummary(memObj);
                return true;
            }
        });
    }

    /**
     * Called when we're no longer hosting the game in question. Called by the GameGameRegistry and
     * ourselves.
     */
    public void clearGame (int gameId)
    {
        if (!_games.remove(gameId)) {
            log.warning("Requested to clear game that we're not hosting?", "game", gameId);
        } else {
            log.info("No longer hosting game", "game", gameId);
            _peerMan.gameDidShutdown(gameId);
        }
    }

    // from interface WorldGameProvider
    public void locateGame (ClientObject caller, final int gameId,
                            WorldGameService.LocationListener listener)
        throws InvocationException
    {
        // if we're already hosting this game, then report back immediately
        if (_games.contains(gameId)) {
            listener.gameLocated(ServerConfig.serverHost, ServerConfig.serverPorts[0]);
            return;
        }

        // otherwise check to see if someone else is hosting this game
        if (checkAndSendToNode(gameId, listener)) {
            return;
        }

        // either we're resolving it, or we're going to start resolving it
        GameResolver resolver = _penders.get(gameId);
        if (resolver == null) {
            _penders.put(gameId, resolver = new GameResolver(gameId));
            resolver.lners.add(listener); // needs to be added before start()
            resolver.start();
        } else {
            resolver.lners.add(listener);
        }
    }

    // from interface MsoyGameProvider
    public void inviteFriends (ClientObject caller, final int gameId, final int[] friendIds)
    {
        final MemberObject memobj = (MemberObject)caller;

        // sanity check; if this breaks some day in real usage, I will be amused
        if (friendIds.length > 255) {
            log.warning("Received crazy invite friends request", "from", memobj.who(),
                        "gameId", gameId, "friendCount", friendIds.length);
            return;
        }

        // check that friends are caller's friends? do we care? maybe we want to allow invites from
        // anyone not just friends...

        // load up the game's name; hello database!
        String name = "inviteFriends(" + gameId + ", " + StringUtil.toString(friendIds) + ")";
        _invoker.postUnit(new RepositoryUnit(name) {
            public void invokePersist () throws Exception {
                GameInfoRecord grec = _mgameRepo.loadGame(gameId);
                if (grec == null) {
                    throw new Exception("No record for game."); // the standard logging is good
                }
                _game = grec.name;
            }
            public void handleSuccess () {
                for (int friendId : friendIds) {
                    _peerMan.invokeNodeAction(
                        new InviteNodeAction(friendId, memobj.memberName, gameId, _game));
                }
            }
            protected String _game;
        });
    }

    // from interface MsoyGameProvider
    public void getTablesWaiting (ClientObject caller, InvocationService.ResultListener rl)
        throws InvocationException
    {
        // add all the tables to a list
        final List<TablesWaiting> games = Lists.newArrayList();
        _peerMan.applyToNodes(new Function<NodeObject,Void>() {
            public Void apply (NodeObject nodeObj) {
                Iterables.addAll(games, ((MsoyNodeObject) nodeObj).tablesWaiting);
                return null; // Void
            }
        });
        // randomize
        Collections.shuffle(games);
        // only return up to 100 or whatever
        int size = games.size();
        if (size > MAX_TABLES_WAITING) {
            games.subList(MAX_TABLES_WAITING, size).clear();
        }
        rl.requestProcessed(games);
    }

    // from interface MsoyPeerManager.PeerObserver
    public void connectedToPeer (MsoyNodeObject peerobj)
    {
        // if the peer that just connected to us claims to be hosting any games that we also claim
        // to be hosting, drop them
        MsoyNodeObject ourobj = (MsoyNodeObject)_peerMan.getNodeObject();
        ArrayIntSet gamesToDrop = new ArrayIntSet();
        for (HostedGame game : peerobj.hostedGames) {
            if (ourobj.hostedGames.contains(game)) {
                log.warning("Zoiks! Peer is hosting the same game as us. Dropping!", "game", game);
                gamesToDrop.add(game.placeId);
            }
        }
        for (int gameId : gamesToDrop) {
            clearGame(gameId);
        }
    }

    // from interface MsoyPeerManager.PeerObserver
    public void disconnectedFromPeer (String node)
    {
        // nada
    }

    protected boolean checkAndSendToNode (int gameId, WorldGameService.LocationListener listener)
    {
        Tuple<String, HostedGame> rhost = _peerMan.getGameHost(gameId);
        if (rhost == null) {
            return false;
        }

        // log.info("Sending game player to " + rhost.left + ":" + rhost.right + ".");
        listener.gameLocated(_peerMan.getPeerPublicHostName(rhost.left),
                             _peerMan.getPeerPort(rhost.left));
        return true;
    }

    protected void resolveGame (final int gameId, final GameResolver resolver)
    {
        // we're going to need the Game item to finish resolution
        _invoker.postUnit(new RepositoryUnit("resolveGame") {
            public void invokePersist () throws Exception {
                _game = _mgameRepo.loadGame(gameId);
            }
            public void handleSuccess () {
                if (_game == null) {
                    log.warning("Requested to resolve unknown game", "game", gameId);
                    resolver.fail();
                } else {
                    hostGame(gameId, _game.name, resolver);
                }
            }
            public void handleFailure (Exception e) {
                log.warning("Failed to resolve game", "game", gameId, e);
                resolver.fail();
            }
            protected GameInfoRecord _game;
        });
    }

    protected void hostGame (int gameId, String name, GameResolver resolver)
    {
        if (!_games.add(gameId)) {
            log.warning("Requested to host game that we're already hosting?", "game", gameId);
        } else {
            log.info("Hosting game", "game", gameId, "name", name);
            _peerMan.gameDidStartup(gameId, name);
        }
        // this will notify the waiting listeners, release our lock and clean itself up
        resolver.finish(ServerConfig.serverHost, ServerConfig.serverPorts[0]);
    }

    /** Handles dispatching invitations to users wherever they may be. */
    protected static class InviteNodeAction extends MemberNodeAction
    {
        public InviteNodeAction (int memberId, MemberName inviter, int gameId, String game) {
            super(memberId);
            _inviter = inviter;
            _gameId = gameId;
            _game = game;
        }

        public InviteNodeAction () {
        }

        protected void execute (MemberObject tgtobj) {
            _notifyMan.notifyGameInvite(tgtobj, _inviter, _game, _gameId);
        }

        protected MemberName _inviter;
        protected int _gameId;
        protected String _game;
        @Inject protected transient NotificationManager _notifyMan;
    }

    protected class GameResolver implements ResultListener<String>
    {
        public final List<WorldGameService.LocationListener> lners = Lists.newArrayList();

        public GameResolver (int gameId) {
            _gameId = gameId;
        }

        public void start () {
            _peerMan.acquireLock(MsoyPeerManager.getGameLock(_gameId), this);
        }

        public void finish (String host, int port) {
            for (WorldGameService.LocationListener listener : lners) {
                listener.gameLocated(host, port);
            }
            clear();
        }

        public void requestCompleted (String nodeName) {
            if (_peerMan.getNodeObject().nodeName.equals(nodeName)) {
                log.debug("Got lock, resolving", "game", _gameId);
                _lock = MsoyPeerManager.getGameLock(_gameId); // note that we got the lock
                resolveGame(_gameId, this);

            } else if (nodeName != null) {
                // some other peer got the lock before we could; send them there
                log.debug("Didn't get lock", "game", _gameId, "remote", nodeName);
                Tuple<String, HostedGame> rhost = _peerMan.getGameHost(_gameId);
                if (rhost != null) {
                    finish(_peerMan.getPeerPublicHostName(rhost.left),
                           _peerMan.getPeerPort(rhost.left));
                } else {
                    log.warning("Failed to acquire lock but no registered host for game!?",
                                "game", _gameId);
                    fail();
                }

            } else {
                log.warning("Game lock acquired by null?", "game", _gameId);
                fail();
            }
        }

        public void requestFailed (Exception cause) {
            log.warning("Failed to acquire game resolution lock", "game", _gameId, cause);
            fail();
        }

        public void fail () {
            for (WorldGameService.LocationListener listener : lners) {
                listener.requestFailed(GameCodes.INTERNAL_ERROR);
            }
            clear();
        }

        protected void clear ()
        {
            // clear ourselves from the penders table
            _penders.remove(_gameId);

            // if we got the lock, we need to release it
            if (_lock != null) {
                _peerMan.releaseLock(_lock, new ResultListener.NOOP<String>());
            }
        }

        protected int _gameId;
        protected MsoyNodeObject.Lock _lock; // filled in if we get the lock
    }

    /** The maximum numbers of TablesWaiting objects we return to a client. */
    protected static final int MAX_TABLES_WAITING = 50;

    /** A map of all games hosted on this server. */
    protected ArrayIntSet _games = new ArrayIntSet();

    /** A map of games pending resolution. */
    protected IntMap<GameResolver> _penders = IntMaps.newHashIntMap();

    // dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected BodyManager _bodyMan;
    @Inject protected Injector _injector;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected PlaceRegistry _placeReg;
}
