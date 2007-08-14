//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.parlor.game.data.GameCodes;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.data.GameSummary;

import static com.threerings.msoy.Log.log;

/**
 * Manages the process of starting up external game server processes and coordinating with them as
 * they host lobbies and games.
 */
public class MsoyGameRegistry
    implements MsoyGameProvider, GameServerProvider, MsoyServer.Shutdowner
{
    /** The invocation services group for game server services. */
    public static final String GAME_SERVER_GROUP = "game_server";

    /**
     * Initializes this registry.
     */
    public void init (InvocationManager invmgr, GameRepository gameRepo)
    {
        _gameRepo = gameRepo;
        invmgr.registerDispatcher(new MsoyGameDispatcher(this), MsoyCodes.GAME_GROUP);
        invmgr.registerDispatcher(new GameServerDispatcher(this), GAME_SERVER_GROUP);

        // register to hear when the server is shutdown
        MsoyServer.registerShutdowner(this);

        // start up our servers after the rest of server initialization is completed (and we know
        // that we're listening for client connections)
        MsoyServer.invoker.postUnit(new Invoker.Unit("startGameServers") {
            public boolean invoke () {
                // start up our game server handlers (and hence our game servers)
                for (int ii = 0; ii < _handlers.length; ii++) {
                    int port = ServerConfig.gameServerPort + ii;
                    try {
                        _handlers[ii] = new GameServerHandler(port);
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Failed to start up game server " +
                                "[port=" + port + "].", e);
                    }
                }
                return false;
            }
        });
    }

    // from interface MsoyGameProvider
    public void locateGame (ClientObject caller, final int gameId,
                            MsoyGameService.LocationListener listener)
        throws InvocationException
    {
        // if we're already hosting this game, then report back immediately
        GameServerHandler handler = _handmap.get(gameId);
        if (handler != null) {
            listener.gameLocated(ServerConfig.serverHost, handler.port);
            return;
        }

        // otherwise check to see if someone else is hosting this game
        if (checkAndSendToNode(gameId, listener)) {
            return;
        }

        // we're going to need the Game item to finish resolution
        MsoyServer.invoker.postUnit(new PersistingUnit("locateGame", listener) {
            public void invokePersistent () throws PersistenceException {
                GameRecord grec = _gameRepo.loadItem(gameId);
                _game = (grec == null) ? null : (Game)grec.toItem();
            }
            public void handleSuccess () {
                lockGame(_game, (MsoyGameService.LocationListener)_listener);
            }
            protected Game _game;
        });
    }

    // from interface GameServerProvider
    public void sayHello (ClientObject caller, int port)
    {
        if (!checkCallerAccess(caller, "sayHello(" + port + ")")) {
            return;
        }

        for (GameServerHandler handler : _handlers) {
            if (handler != null && handler.port == port) {
                handler.setClientObject(caller);
                return;
            }
        }

        log.warning("Got hello from unknown game server [port=" + port + "].");
    }

    // from interface GameServerProvider
    public void updatePlayer (ClientObject caller, int playerId, GameSummary game)
    {
        if (!checkCallerAccess(caller, "updatePlayer(" + playerId + ")")) {
            return;
        }

        MemberObject memobj = MsoyServer.lookupMember(playerId);
        if (memobj == null) {
            // they went bye bye, oh well
            log.info("Dropping update for departee [pid=" + playerId + ", game=" + game + "].");
            return;
        }

        // set or clear their pending game
        memobj.setPendingGame(game);

        // update their occupant info if they're in a scene
        MsoyServer.memberMan.updateOccupantInfo(memobj);

        // update their published location in our peer object
        MsoyServer.peerMan.updateMemberLocation(memobj);
    }

    // from interface GameServerProvider
    public void clearGameHost (ClientObject caller, int port, int gameId)
    {
        if (!checkCallerAccess(caller, "clearGameHost(" + port + ", " + gameId + ")")) {
            return;
        }

        for (GameServerHandler handler : _handlers) {
            if (handler != null && handler.port == port) {
                handler.clearGame(gameId);
                return;
            }
        }
        log.warning("Game cleared by unknown handler? [port=" + port + ", id=" + gameId + "].");
    }

    // from interface GameServerProvider
    public void reportFlowAward (ClientObject caller, int memberId, int deltaFlow)
    {
        if (!checkCallerAccess(caller, "reportFlowAward(" + memberId + ", " + deltaFlow + ")")) {
            return;
        }

        MemberObject mobj = MsoyServer.lookupMember(memberId);
        if (mobj != null) {
            mobj.setFlow(mobj.flow + deltaFlow);
            mobj.setAccFlow(mobj.flow + deltaFlow);

        } else {
            // TODO: locate the peer that is hosting this member and forward the flow update there
        }
    }

    // from interface MsoyServer.Shutdowner
    public void shutdown ()
    {
        // shutdown our game server handlers
        for (GameServerHandler handler : _handlers) {
            if (handler != null) {
                handler.shutdown();
            }
        }
    }

    protected boolean checkAndSendToNode (int gameId, MsoyGameService.LocationListener listener)
    {
        Tuple<String, Integer> rhost = MsoyServer.peerMan.getGameHost(gameId);
        if (rhost == null) {
            return false;
        }

        String hostname = MsoyServer.peerMan.getPeerPublicHostName(rhost.left);
        log.info("Sending game player to " + rhost.left + ":" + rhost.right + ".");
        listener.gameLocated(hostname, rhost.right);
        return true;
    }

    protected void lockGame (final Game game, final MsoyGameService.LocationListener listener)
    {
        // otherwise obtain a lock and resolve the game ourselves
        MsoyServer.peerMan.acquireLock(
            MsoyPeerManager.getGameLock(game.itemId), new ResultListener<String>() {
            public void requestCompleted (String nodeName) {
                if (MsoyServer.peerMan.getNodeObject().nodeName.equals(nodeName)) {
                    log.info("Got lock, resolving " + game.name + ".");
                    hostGame(game, listener);

                } else if (nodeName != null) {
                    // some other peer got the lock before we could; send them there
                    log.info("Didn't get lock, going remote " + game.itemId + "@" + nodeName + ".");
                    if (!checkAndSendToNode(game.itemId, listener)) {
                        log.warning("Falied to acquire lock but no registered host for game!? " +
                                    "[id=" + game.itemId + "].");
                        listener.requestFailed(GameCodes.INTERNAL_ERROR);
                    }

                } else {
                    log.warning("Game lock acquired by null? [id=" + game.itemId + "].");
                    listener.requestFailed(GameCodes.INTERNAL_ERROR);
                }
            }

            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Failed to acquire game resolution lock " +
                        "[id=" + game.itemId + "].", cause);
                listener.requestFailed(GameCodes.INTERNAL_ERROR);
            }
        });
    }

    protected void hostGame (Game game, MsoyGameService.LocationListener listener)
    {
        // TODO: load balance across our handlers if we ever have more than one
        GameServerHandler handler = _handlers[0];
        if (handler == null) {
            log.warning("Have no game servers, cannot handle game [id=" + game.itemId + "].");
            listener.requestFailed(GameCodes.INTERNAL_ERROR);

            // releases our lock on this game as we didn't end up hosting it
            MsoyServer.peerMan.releaseLock(MsoyPeerManager.getGameLock(game.itemId),
                                           new ResultListener.NOOP<String>());
            return;
        }

        // register this handler as handling this game
        handler.hostGame(game);
        _handmap.put(game.itemId, handler);

        listener.gameLocated(ServerConfig.serverHost, handler.port);
    }

    protected boolean checkCallerAccess (ClientObject caller, String method)
    {
        // peers will not have member objects and server local calls will be a null caller
        if (caller instanceof MemberObject) {
            log.warning("Rejecting non-peer caller of " + method +
                        " [who=" + ((MemberObject)caller).who() + "].");
            return false;
        }
        return true;
    }

    /** Handles communications with a delegate game server. */
    protected static class GameServerHandler
    {
        public int port;

        public GameServerHandler (int port) throws Exception {
            // make a note of our port
            this.port = port;

            // the rungame script explicitly redirects all output, so we don't need to worry about
            // this process's input or output streams
            Runtime.getRuntime().exec(new String[] {
                ServerConfig.serverRoot + "/bin/rungame",
                String.valueOf(port),
            });
        }

        public void setClientObject (ClientObject clobj) {
            _clobj = clobj;
            // TODO: if our client object is destroyed and we aren't shutting down, restart the
            // game server?
        }

        public void hostGame (Game game) {
            if (!_games.add(game.itemId)) {
                log.warning("Requested to host game that we're already hosting? [port=" + port +
                            ", game=" + game.itemId + "].");
            } else {
                MsoyServer.peerMan.gameDidStartup(game.itemId, game.name, port);
            }
        }

        public void clearGame (int gameId) {
            if (!_games.remove(gameId)) {
                log.warning("Requested to clear game that we're not hosting? [port=" + port +
                            ", game=" + gameId + "].");
            } else {
                MsoyServer.peerMan.gameDidShutdown(gameId);
            }
        }

        public void shutdown () {
            if (_clobj != null && _clobj.isActive()) {
                log.info("Shutting down game server " + port + "...");
                _clobj.postMessage(WorldServerClient.SHUTDOWN_MESSAGE);
            } else {
                log.info("Not shutting down game server " + port + "...");
            }
        }

        protected ClientObject _clobj;
        protected ArrayIntSet _games = new ArrayIntSet();
    }

    /** Used to load metadata for games. */
    protected GameRepository _gameRepo;

    /** Handlers for our delegate game servers. */
    protected GameServerHandler[] _handlers = new GameServerHandler[DELEGATE_GAME_SERVERS];

    /** Contains a mapping from gameId to handler for all game servers hosted on this machine. */
    protected HashIntMap<GameServerHandler> _handmap = new HashIntMap<GameServerHandler>();

    /** The number of delegate game servers to be started. */
    protected static final int DELEGATE_GAME_SERVERS = 1;
}
