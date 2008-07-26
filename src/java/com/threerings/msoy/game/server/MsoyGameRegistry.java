//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.ProcessLogger;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.util.PersistingUnit;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.parlor.game.data.GameCodes;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.world.server.RoomManager;

import com.threerings.msoy.bureau.data.ServerRegistryObject;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.StaticMediaDesc;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.game.client.GameServerService;
import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.all.Trophy;

import static com.threerings.msoy.Log.log;

/**
 * Manages the process of starting up external game server processes and coordinating with them as
 * they host lobbies and games.
 */
@Singleton
public class MsoyGameRegistry
    implements MsoyGameProvider, GameServerProvider, ShutdownManager.Shutdowner
{
    /** The invocation services group for game server services. */
    public static final String GAME_SERVER_GROUP = "game_server";

    /** A predefined game record for our tutorial game. */
    public static final Game TUTORIAL_GAME = new Game() {
        /* implicit constructor */ {
            this.gameId = TUTORIAL_GAME_ID;
            this.name = "Whirled Tutorial";
            this.config = "<avrg/>";
            this.gameMedia = new StaticMediaDesc(
                MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.GAME, "tutorial");
            // TODO: if we end up using these for AVRG's we'll want hand-crafted stuffs here
            this.thumbMedia = getDefaultThumbnailMediaFor(GAME);
            this.furniMedia = getDefaultFurniMediaFor(GAME);
        }
    };

    @Inject public MsoyGameRegistry (ShutdownManager shutmgr, InvocationManager invmgr)
    {
        shutmgr.registerShutdowner(this);
        invmgr.registerDispatcher(new MsoyGameDispatcher(this), MsoyCodes.GAME_GROUP);
        invmgr.registerDispatcher(new GameServerDispatcher(this), GAME_SERVER_GROUP);
    }

    /**
     * Initializes this registry and queues up our game servers to be started.
     */
    public void init ()
    {
        _serverRegObj = new ServerRegistryObject();
        _omgr.registerObject(_serverRegObj);

        // start up our servers after the rest of server initialization is completed (and we know
        // that we're listening for client connections)
        _omgr.postRunnable(new PresentsDObjectMgr.LongRunnable() {
            public void run () {
                // start up our game server handlers (and hence our game servers)
                for (int ii = 0; ii < _handlers.length; ii++) {
                    int port = ServerConfig.gameServerPort + ii;
                    try {
                        _handlers[ii] = new GameServerHandler(port);
                    } catch (Exception e) {
                        log.warning("Failed to start up game server " +
                                "[port=" + port + "].", e);
                    }
                }
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
        _invoker.postUnit(new PersistingUnit("locateGame", listener) {
            public void invokePersistent () throws PersistenceException {
                if (gameId == Game.TUTORIAL_GAME_ID) {
                    _game = TUTORIAL_GAME;
                } else {
                    GameRecord grec = _gameRepo.loadGameRecord(gameId);
                    _game = (grec == null) ? null : (Game)grec.toItem();
                }
            }
            public void handleSuccess () {
                if (_game == null) {
                    log.warning("Requested to locate unknown game [id=" + gameId + "].");
                    _listener.requestFailed(GameCodes.INTERNAL_ERROR);
                } else {
                    lockGame(_game, (MsoyGameService.LocationListener)_listener);
                }
            }
            protected Game _game;
        });
    }

    // from interface MsoyGameProvider
    public void inviteFriends (ClientObject caller, final int gameId, final int[] friendIds)
    {
        final MemberObject memobj = (MemberObject)caller;

        // sanity check; if this breaks some day in real usage, I will be amused
        if (friendIds.length > 255) {
            log.warning("Received crazy invite friends request [from=" + memobj.who() +
                        ", gameId=" + gameId + ", friendCount=" + friendIds.length + "].");
            return;
        }

        // check that friends are caller's friends? do we care? maybe we want to allow invites from
        // anyone not just friends...

        // load up the game's name; hello database!
        String name = "inviteFriends(" + gameId + ", " + StringUtil.toString(friendIds) + ")";
        _invoker.postUnit(new RepositoryUnit(name) {
            public void invokePersist () throws Exception {
                GameRecord grec = _itemMan.getGameRepository().loadGameRecord(gameId);
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

    // from interface GameServerProvider
    public void sayHello (ClientObject caller, int port)
    {
        if (!checkCallerAccess(caller, "sayHello(" + port + ")")) {
            return;
        }

        for (GameServerHandler handler : _handlers) {
            if (handler != null && handler.port == port) {
                handler.setClientObject(caller);
                _serverRegObj.addToServers(new ServerRegistryObject.ServerInfo(
                    ServerConfig.serverHost, port));
                return;
            }
        }

        log.warning("Got hello from unknown game server [port=" + port + "].");
    }

    // from interface GameServerProvider
    public void leaveAVRGame (ClientObject caller, int playerId)
    {
        if (!checkCallerAccess(caller, "leaveAVRGame(" + playerId + ")")) {
            return;
        }
        _peerMan.invokeNodeAction(new LeaveAVRGameAction(playerId));
    }

    // from interface GameServerProvider
    public void updatePlayer (ClientObject caller, int playerId, GameSummary game)
    {
        if (!checkCallerAccess(caller, "updatePlayer(" + playerId + ")")) {
            return;
        }
        _peerMan.invokeNodeAction(new UpdatePlayerAction(playerId, game));
    }

    // from interface GameServerProvider
    public void reportFlowAward (ClientObject caller, int memberId, int deltaCoins)
    {
        if (!checkCallerAccess(caller, "reportFlowAward(" + memberId + ", " + deltaCoins + ")")) {
            return;
        }
        _peerMan.invokeNodeAction(new ReportCoinsAwardAction(memberId, deltaCoins));
    }

    // from interface GameServerProvider
    public void clearGameHost (ClientObject caller, int port, int gameId)
    {
        if (!checkCallerAccess(caller, "clearGameHost(" + port + ", " + gameId + ")")) {
            return;
        }

        GameServerHandler handler = _handmap.remove(gameId);
        if (handler != null) {
            handler.clearGame(gameId);
        } else {
            log.warning("Game cleared by unknown handler? [port=" + port + ", id=" + gameId + "].");
        }
    }

    /**
     * Called to update that a player is either lobbying for, playing, or no longer playing
     * the specified game.
     */
    public void updatePlayerOnPeer (MemberObject memObj, GameSummary game)
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
                    memObj.setAvrGameId(game.gameId);

                    // and immediately let the room manager give us of control, if needed
                    if (rmgr != null && game.gameId != avrGameId) {
                        rmgr.occupantEnteredAVRGame(memObj);
                    }
                }
            }
        } finally {
            memObj.commitTransaction();
        }

        // update their occupant info if they're in a scene
        _memberMan.updateOccupantInfo(memObj);

        // update their published location in our peer object
        _peerMan.updateMemberLocation(memObj);
    }

    /**
     * Called to update a game.
     */
    public void gameUpdatedOnPeer (int gameId)
    {
        GameServerHandler handler = _handmap.get(gameId);
        if (handler == null) {
            log.info("Eek, handler vanished [gameId=" + gameId + "]");
            return;
        }
        handler.postMessage(WorldServerClient.GAME_RECORD_UPDATED, gameId);
    }

    /**
     * Forwards a request to our game server to have the specified resolved game reset its
     * percentiler score trackers in memory.
     */
    public void resetGameScores (int gameId, boolean single)
    {
        GameServerHandler handler = _handmap.get(gameId);
        if (handler == null) {
            log.info("Eek, handler vanished [gameId=" + gameId + "]");
            return;
        }
        handler.postMessage(WorldServerClient.RESET_SCORE_PERCENTILER, gameId, single);
    }

    // from interface GameServerProvider
    public void reportTrophyAward (
        ClientObject caller, int memberId, String gameName, Trophy trophy)
    {
        if (!checkCallerAccess(caller, "reportTrophyAward(" + memberId + ", " + gameName + ")")) {
            return;
        }

// TODO: put this in their feed
//         // send them a mail message as well
//         String subject = _serverMsgs.getBundle("server").get(
//             "m.got_trophy_subject", trophy.name);
//         String body = _serverMsgs.getBundle("server").get(
//             "m.got_trophy_body", trophy.description);
//         _mailMan.deliverMessage(
//             // TODO: sender should be special system id
//             memberId, memberId, subject, body, new GameAwardPayload(
//                 trophy.gameId, gameName, GameAwardPayload.TROPHY, trophy.name, trophy.trophyMedia),
//             true, new ResultListener.NOOP<Void>());
    }

    // from interface GameServerProvider
    public void awardPrize (ClientObject caller, int memberId, int gameId, String gameName,
                            Prize prize, GameServerService.ResultListener listener)
        throws InvocationException
    {
        if (!checkCallerAccess(caller, "awardPrize(" + memberId + ", " + prize.ident + ")")) {
            return;
        }
        // pass the buck to the item manager
        _itemMan.awardPrize(memberId, gameId, gameName, prize, new ResultAdapter<Item>(listener));
    }

    // from interface _Shutdowner
    public void shutdown ()
    {
        // shutdown our game server handlers
        for (GameServerHandler handler : _handlers) {
            if (handler != null) {
                handler.shutdown();
            }
        }
    }

    public ServerRegistryObject getServerRegistryObject ()
    {
        return _serverRegObj;
    }

    protected boolean checkAndSendToNode (int gameId, MsoyGameService.LocationListener listener)
    {
        Tuple<String, Integer> rhost = _peerMan.getGameHost(gameId);
        if (rhost == null) {
            return false;
        }

        String hostname = _peerMan.getPeerPublicHostName(rhost.left);
        log.info("Sending game player to " + rhost.left + ":" + rhost.right + ".");
        listener.gameLocated(hostname, rhost.right);
        return true;
    }

    protected void lockGame (final Game game, final MsoyGameService.LocationListener listener)
    {
        // otherwise obtain a lock and resolve the game ourselves
        _peerMan.acquireLock(MsoyPeerManager.getGameLock(game.gameId),
                             new ResultListener<String>() {
            public void requestCompleted (String nodeName) {
                if (_peerMan.getNodeObject().nodeName.equals(nodeName)) {
                    log.info("Got lock, resolving " + game.name + ".");
                    hostGame(game, listener);

                } else if (nodeName != null) {
                    // some other peer got the lock before we could; send them there
                    log.info("Didn't get lock, going remote " + game.gameId + "@" + nodeName + ".");
                    if (!checkAndSendToNode(game.gameId, listener)) {
                        log.warning("Failed to acquire lock but no registered host for game!? " +
                                    "[id=" + game.gameId + "].");
                        listener.requestFailed(GameCodes.INTERNAL_ERROR);
                    }

                } else {
                    log.warning("Game lock acquired by null? [id=" + game.gameId + "].");
                    listener.requestFailed(GameCodes.INTERNAL_ERROR);
                }
            }

            public void requestFailed (Exception cause) {
                log.warning("Failed to acquire game resolution lock " +
                        "[id=" + game.gameId + "].", cause);
                listener.requestFailed(GameCodes.INTERNAL_ERROR);
            }
        });
    }

    protected void hostGame (Game game, MsoyGameService.LocationListener listener)
    {
        // TODO: load balance across our handlers if we ever have more than one
        GameServerHandler handler = _handlers[0];
        if (handler == null) {
            log.warning("Have no game servers, cannot handle game [id=" + game.gameId + "].");
            listener.requestFailed(GameCodes.INTERNAL_ERROR);

            // releases our lock on this game as we didn't end up hosting it
            _peerMan.releaseLock(MsoyPeerManager.getGameLock(game.gameId),
                                 new ResultListener.NOOP<String>());
            return;
        }

        // register this handler as handling this game
        handler.hostGame(game);
        _handmap.put(game.gameId, handler);

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
    protected class GameServerHandler
    {
        public int port;

        public GameServerHandler (int port) throws Exception {
            // make a note of our port
            this.port = port;

            String exec[] = {
                ServerConfig.serverRoot + "/bin/rungame",
                String.valueOf(port),
                // have the game server connect to us on our first port
                String.valueOf(ServerConfig.serverPorts[0]),
            };

            // fire up the game server process
            Process proc = Runtime.getRuntime().exec(exec);

            log.info("Launched process " + StringUtil.toString(exec));

            // copy stdout and stderr to our logs
            ProcessLogger.copyOutput(log, "rungame", proc);
        }

        public void setClientObject (ClientObject clobj) {
            _clobj = clobj;
            // TODO: if our client object is destroyed and we aren't shutting down, restart the
            // game server?
        }

        public void hostGame (Game game) {
            if (!_games.add(game.gameId)) {
                log.warning("Requested to host game that we're already hosting? [port=" + port +
                            ", game=" + game.gameId + "].");
            } else {
                _peerMan.gameDidStartup(game.gameId, game.name, port);
            }
        }

        public void clearGame (int gameId) {
            if (!_games.remove(gameId)) {
                log.warning("Requested to clear game that we're not hosting? [port=" + port +
                            ", game=" + gameId + "].");
            } else {
                _peerMan.gameDidShutdown(gameId);
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

        public void postMessage (String name, Object... args)
        {
            _clobj.postMessage(name, args);
        }

        protected ClientObject _clobj;
        protected ArrayIntSet _games = new ArrayIntSet();
    }

    /** Handles dispatching invitations to users wherever they may be. */
    protected static class InviteNodeAction extends MemberNodeAction
    {
        public InviteNodeAction (int memberId, MemberName inviter, int gameId, String game) {
            super(memberId);
            _inviterId = inviter.getMemberId();
            _inviter = inviter.toString();
            _gameId = gameId;
            _game = game;
        }

        protected void execute (MemberObject tgtobj) {
            _notifyMan.notifyGameInvite(tgtobj, _inviter, _inviterId, _game, _gameId);
        }

        protected int _inviterId, _gameId;
        protected String _inviter, _game;
        @Inject protected transient NotificationManager _notifyMan;
    }

    /** Handles updating a player's game. */
    protected static class UpdatePlayerAction extends MemberNodeAction
    {
        public UpdatePlayerAction (int memberId, GameSummary game) {
            super(memberId);
            _game = game;
        }

        protected void execute (MemberObject memObj) {
            _gameReg.updatePlayerOnPeer(memObj, _game);
        }

        protected GameSummary _game;
        @Inject protected transient MsoyGameRegistry _gameReg;
    }

    /** Handles leaving an AVR game. */
    protected static class LeaveAVRGameAction extends MemberNodeAction
    {
        public LeaveAVRGameAction (int memberId) {
            super(memberId);
        }

        protected void execute (MemberObject memObj) {
            // clear their AVRG affiliation
            memObj.setAvrGameId(0);
        }
    }

    /** Handles updating a player's coin count. */
    protected static class ReportCoinsAwardAction extends MemberNodeAction
    {
        public ReportCoinsAwardAction (int memberId, int deltaCoins) {
            super(memberId);
            _deltaCoins = deltaCoins;
        }

        protected void execute (MemberObject memObj) {
            memObj.setFlow(memObj.flow + _deltaCoins);
            memObj.setAccFlow(memObj.accFlow + _deltaCoins);
        }

        protected int _deltaCoins;
    }

    /** Hold distributed information about our game servers. */
    protected ServerRegistryObject _serverRegObj;

    /** Handlers for our delegate game servers. */
    protected GameServerHandler[] _handlers = new GameServerHandler[DELEGATE_GAME_SERVERS];

    /** Contains a mapping from gameId to handler for all game servers hosted on this machine. */
    protected HashIntMap<GameServerHandler> _handmap = new HashIntMap<GameServerHandler>();

    // dependencies
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected PlaceRegistry _placeReg;
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected MemberManager _memberMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected GameRepository _gameRepo;

    /** The number of delegate game servers to be started. */
    protected static final int DELEGATE_GAME_SERVERS = 1;
}
