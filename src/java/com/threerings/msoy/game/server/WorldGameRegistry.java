//
// $Id$

package com.threerings.msoy.game.server;

import java.util.Collection;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ProcessLogger;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.ClientManager.ClientObserver;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.net.ConnectionManager;
import com.threerings.presents.util.PersistingUnit;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.crowd.server.BodyManager;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.game.data.GameCodes;
import com.threerings.stats.data.StatModifier;

import com.threerings.util.Name;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.AuxSessionFactory;
import com.threerings.msoy.server.MemberLogic;
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

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.data.GameCredentials;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.all.Trophy;
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
        _serverRegObj = new ServerRegistryObject();
        _omgr.registerObject(_serverRegObj);

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
     * Called to update that a player is either lobbying for, playing, or no longer playing
     * the specified game.
     */
    public void updatePlayerOnPeer (final MemberObject memObj, GameSummary game)
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
        _bodyMan.updateOccupantInfo(memObj, new MemberInfo.Updater<MemberInfo>() {
            public boolean update (MemberInfo info) {
                info.updateGameSummary(memObj);
                return true;
            }
        });

        // update their published location in our peer object
        _peerMan.updateMemberLocation(memObj);
    }

    // from interface MsoyGameProvider
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

    /**
     * Called when we're no longer hosting the game in question. Called by the GameGameRegistry and
     * ourselves.
     */
    public void clearGame (int gameId)
    {
        if (!_games.remove(gameId)) {
            log.warning("Requested to clear game that we're not hosting?", "game", gameId);
        } else {
            _peerMan.gameDidShutdown(gameId);
        }
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
        if (!_games.add(game.gameId)) {
            log.warning("Requested to host game that we're already hosting?", "game", game.gameId);
        } else {
            _peerMan.gameDidStartup(game.gameId, game.name);
        }
        listener.gameLocated(ServerConfig.serverHost, ServerConfig.serverPorts[0]);
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

    /** Hold distributed information about our game servers. */
    protected ServerRegistryObject _serverRegObj;

    /** A map of all games hosted on this server. */
    protected ArrayIntSet _games = new ArrayIntSet();

    // dependencies
    @Inject protected Injector _injector;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected ShutdownManager _shutMan;
    @Inject protected BodyManager _bodyMan;
    @Inject protected PlaceRegistry _placeReg;
    @Inject protected ItemManager _itemMan;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected StatLogic _statLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected ClientManager _clmgr;
}
