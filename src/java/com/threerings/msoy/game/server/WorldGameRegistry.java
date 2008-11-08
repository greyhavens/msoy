//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.util.PersistingUnit;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.parlor.game.data.GameCodes;
import com.threerings.stats.data.StatModifier;

import com.threerings.util.Name;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.StatLogic;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.bureau.data.ServerRegistryObject;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.notify.server.NotificationManager;

import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.game.client.GameServerService;
import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.room.server.RoomManager;

import static com.threerings.msoy.Log.log;

/**
 * Manages the process of starting up external game server processes and coordinating with them as
 * they host lobbies and games.
 */
@Singleton
public class WorldGameRegistry
    implements WorldGameProvider, GameServerProvider, ShutdownManager.Shutdowner,
               MsoyPeerManager.PeerObserver
{
    /** The invocation services group for game server services. */
    public static final String GAME_SERVER_GROUP = "game_server";

    @Inject public WorldGameRegistry (ShutdownManager shutmgr, InvocationManager invmgr)
    {
        shutmgr.registerShutdowner(this);
        invmgr.registerDispatcher(new WorldGameDispatcher(this), MsoyCodes.GAME_GROUP);
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
                        log.warning("Failed to start up game server", "port", port, e);
                    }
                }
            }
        });

        // listen for peer connections so that we can manage multiply claimed games
        _peerMan.peerObs.add(this);
    }

    /**
     * Returns the object in which we track bureau server registration.
     */
    public ServerRegistryObject getServerRegistryObject ()
    {
        return _serverRegObj;
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

                    // Note we do not clear the MemberObject.avrGameId here because if the player
                    // has not chosen to leave the game (e.g. logged off and on again), we want to
                    // keep it around for the client to decide whether to rejoin
                    // memObj.setAvrGameId(0);
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
     * Called when the persistent data for a game that we host has been updated. Notifies the game
     * server hosting the game in question so that it can reload that game's content.
     */
    public void gameUpdated (int gameId)
    {
        GameServerHandler handler = _handmap.get(gameId);
        if (handler == null) {
            log.info("Eek, handler vanished", "gameId", gameId);
            return;
        }
        handler.postMessage(WorldServerClient.GAME_CONTENT_UPDATED, gameId);
    }

    /**
     * Called when the user has purchased game content.
     *
     * @param itemType the type of content item that was purchased (ie. {@link Item#LEVEL_PACK}).
     * @param ident the string that the game uses internally to identify the content.
     */
    public void gameContentPurchased (int memberId, int gameId, byte itemType, String ident)
    {
        GameServerHandler handler = _handmap.get(gameId);
        if (handler == null) {
            log.info("Egad, the game handler vanished", "gameId", gameId);
            return;
        }
        handler.postMessage(WorldServerClient.GAME_CONTENT_PURCHASED,
                            memberId, gameId, itemType, ident);
    }

    /**
     * Forwards a request to our game server to have the specified resolved game reset its
     * percentiler score trackers in memory.
     */
    public void resetGameScores (int gameId, boolean single)
    {
        GameServerHandler handler = _handmap.get(gameId);
        if (handler == null) {
            log.info("Eek, handler vanished", "gameId", gameId);
            return;
        }
        handler.postMessage(WorldServerClient.RESET_SCORE_PERCENTILER, gameId, single);
    }

    /**
     * Forwards a broadcast to the game server, so that it can be sent on all of that server's
     * place (game) objects.
     */
    public void forwardBroadcast(Name from, String bundle, String msg, boolean attention)
    {
        for (GameServerHandler handler : _handlers) {
            handler.postMessage(WorldServerClient.FORWARD_BROADCAST, from, bundle, msg, attention);
        }
    }

    /**
     * Requests that we instruct our game server to flush any pending coin earnings for the
     * specified member.
     */
    public void flushCoinEarnings (int memberId)
    {
        for (GameServerHandler handler : _handlers) {
            handler.postMessage(WorldServerClient.FLUSH_COIN_EARNINGS, memberId);
        }
    }

    // from interface MsoyGameProvider
    public void locateGame (ClientObject caller, final int gameId,
                            WorldGameService.LocationListener listener)
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
            public void invokePersistent () throws Exception {
                GameRecord grec = _mgameRepo.loadGameRecord(gameId);
                _game = (grec == null) ? null : (Game)grec.toItem();
            }
            public void handleSuccess () {
                if (_game == null) {
                    log.warning("Requested to locate unknown game", "id", gameId);
                    _listener.requestFailed(GameCodes.INTERNAL_ERROR);
                } else {
                    lockGame(_game, (WorldGameService.LocationListener)_listener);
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
                GameRecord grec = _mgameRepo.loadGameRecord(gameId);
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

        log.warning("Got hello from unknown game server", "port", port);
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
    public void reportCoinAward (ClientObject caller, int memberId, int deltaCoins)
    {
        if (!checkCallerAccess(caller, "reportCoinAward(" + memberId + ", " + deltaCoins + ")")) {
            return;
        }
        _moneyLogic.notifyCoinsEarned(memberId, deltaCoins);
    }

    // from interface GameServerProvider
    public void awardCoins (ClientObject caller, int gameId, UserAction action, int amount)
    {
        if (!checkCallerAccess(caller, "awardCoins(" + gameId + ", " + action + ", " + amount)) {
            return;
        }
        _moneyLogic.awardCoins(action.memberId, amount, false, action);
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
            log.warning("Game cleared by unknown handler?", "port", port, "id", gameId);
        }
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
    
    // from interface GameServerProvider
    public void notifyMemberStartedGame (ClientObject caller, final int memberId, final byte action, 
        final int gameId)
    {
        MemberNodeActions.addExperience(memberId, action, gameId);
    }

    // from interface GameServerProvider
    @SuppressWarnings("unchecked")
    public void updateStat (ClientObject caller, int memberId, StatModifier modifier)
    {
        if (!checkCallerAccess(caller, "updateStat(" + memberId + ", " + modifier + ")")) {
            return;
        }
        // stat logic will update the stat in the database and send a MemberNodeAction to the
        // appropriate MemberObject
        _statLogic.updateStat(memberId, modifier);
    }

    // from interface Shutdowner
    public void shutdown ()
    {
        // shutdown our game server handlers
        for (GameServerHandler handler : _handlers) {
            if (handler != null) {
                handler.shutdown();
            }
        }
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
            clearGameHost(null, 0, gameId);
        }
    }

    // from interface MsoyPeerManager.PeerObserver
    public void disconnectedFromPeer (String node)
    {
        // nada
    }

    protected boolean checkAndSendToNode (int gameId, WorldGameService.LocationListener listener)
    {
        Tuple<String, Integer> rhost = _peerMan.getGameHost(gameId);
        if (rhost == null) {
            return false;
        }

        String hostname = _peerMan.getPeerPublicHostName(rhost.left);
        // log.info("Sending game player to " + rhost.left + ":" + rhost.right + ".");
        listener.gameLocated(hostname, rhost.right);
        return true;
    }

    protected void lockGame (final Game game, final WorldGameService.LocationListener listener)
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
                        log.warning("Failed to acquire lock but no registered host for game!?",
                                    "id", game.gameId);
                        listener.requestFailed(GameCodes.INTERNAL_ERROR);
                    }

                } else {
                    log.warning("Game lock acquired by null?", "id", game.gameId);
                    listener.requestFailed(GameCodes.INTERNAL_ERROR);
                }
            }

            public void requestFailed (Exception cause) {
                log.warning("Failed to acquire game resolution lock", "id", game.gameId, cause);
                listener.requestFailed(GameCodes.INTERNAL_ERROR);
            }
        });
    }

    protected void hostGame (Game game, WorldGameService.LocationListener listener)
    {
        // TODO: load balance across our handlers if we ever have more than one
        GameServerHandler handler = _handlers[0];
        if (handler == null) {
            log.warning("Have no game servers, cannot handle game", "id", game.gameId);
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
            log.warning("Rejecting non-peer caller of " + method, "who", caller.who());
            return false;
        }
        return true;
    }

    /** Handles communications with a delegate game server. */
    protected class GameServerHandler
        implements ObjectDeathListener
    {
        public int port;

        public GameServerHandler (int port) throws Exception {
            // make a note of our port
            this.port = port;

            // start up our game server
            startGameServer();
        }

        public void setClientObject (ClientObject clobj) {
            _clobj = clobj;
            _clobj.addListener(this);
        }

        public void hostGame (Game game) {
            if (!_games.add(game.gameId)) {
                log.warning("Requested to host game that we're already hosting?",
                            "port", port, "game", game.gameId);
            } else {
                _peerMan.gameDidStartup(game.gameId, game.name, port);
            }
        }

        public void clearGame (int gameId) {
            if (!_games.remove(gameId)) {
                log.warning("Requested to clear game that we're not hosting?",
                            "port", port, "game", gameId);
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

        public void postMessage (String name, Object... args) {
            _clobj.postMessage(name, args);
        }

        // from interface ObjectDeathListener
        public void objectDestroyed (ObjectDestroyedEvent event) {
            // note that we no longer hosting this server
            _serverRegObj.removeFromServers(
                new ServerRegistryObject.ServerInfo(ServerConfig.serverHost, this.port).getKey());

            // if we're not also shutting down, then restart this game server
            if (!_shutMan.isShuttingDown()) {
                _invoker.postUnit(new Invoker.Unit() {
                    public boolean invoke () {
                        try {
                            startGameServer();
                        } catch (Exception e) {
                            log.warning("Failed to restart failed game server", "port", port, e);
                        }
                        return false;
                    }
                });
            }
        }

        protected void startGameServer () throws Exception {
            String exec[] = {
                ServerConfig.serverRoot + "/bin/rungame",
                String.valueOf(this.port),
                // have the game server connect to us on our first port
                String.valueOf(ServerConfig.serverPorts[0]),
            };

            // fire up the game server process
            Process proc = Runtime.getRuntime().exec(exec);

            log.info("Launched process " + StringUtil.toString(exec));

            // copy stdout and stderr to our logs
            ProcessLogger.copyOutput(log, "rungame", proc);
        }

        protected ClientObject _clobj;
        protected ArrayIntSet _games = new ArrayIntSet();
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

    /** Handles updating a player's game. */
    protected static class UpdatePlayerAction extends MemberNodeAction
    {
        public UpdatePlayerAction (int memberId, GameSummary game) {
            super(memberId);
            _game = game;
        }

        public UpdatePlayerAction () {
        }

        protected void execute (MemberObject memObj) {
            _gameReg.updatePlayerOnPeer(memObj, _game);
        }

        protected GameSummary _game;
        @Inject protected transient WorldGameRegistry _gameReg;
    }

    /** Handles leaving an AVR game. */
    protected static class LeaveAVRGameAction extends MemberNodeAction
    {
        public LeaveAVRGameAction (int memberId) {
            super(memberId);
        }

        public LeaveAVRGameAction () {
        }

        protected void execute (MemberObject memObj) {
            // clear their AVRG affiliation
            memObj.setAvrGameId(0);
        }
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
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected ShutdownManager _shutMan;
    @Inject protected PlaceRegistry _placeReg;
    @Inject protected MemberManager _memberMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected StatLogic _statLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MemberLogic _memberLogic;
    
    /** The number of delegate game servers to be started. */
    protected static final int DELEGATE_GAME_SERVERS = 1;
}
