//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.client.InvocationService.ResultListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.ResultListenerList;

import com.threerings.parlor.data.Table;

import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.rating.util.Percentiler;

import com.whirled.data.GameData;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemPackRecord;
import com.threerings.msoy.item.server.persist.ItemPackRepository;
import com.threerings.msoy.item.server.persist.LevelPackRecord;
import com.threerings.msoy.item.server.persist.LevelPackRepository;
import com.threerings.msoy.item.server.persist.PrizeRecord;
import com.threerings.msoy.item.server.persist.PrizeRepository;
import com.threerings.msoy.item.server.persist.TrophySourceRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.GameContentOwnership;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.server.persist.AVRGameRepository;
import com.threerings.msoy.game.server.persist.GameStateRecord;
import com.threerings.msoy.game.server.persist.PlayerGameStateRecord;
import com.threerings.msoy.game.server.persist.QuestStateRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;

import com.threerings.msoy.server.MsoyBaseServer;

import static com.threerings.msoy.Log.log;

/**
 * Manages the lobbies active on this server.
 */
public class GameGameRegistry
    implements LobbyProvider, AVRProvider,
               MsoyGameServer.Shutdowner, LobbyManager.ShutdownObserver
{
    /**
     * Initializes this registry.
     */
    public void init (RootDObjectManager omgr, InvocationManager invmgr, PersistenceContext perCtx,
                      RatingRepository ratingRepo)
    {
        _omgr = omgr;

        // create our various game-related repositories
        _gameRepo = new GameRepository(perCtx);
        _avrgRepo = new AVRGameRepository(perCtx);
        _trophyRepo = new TrophyRepository(perCtx);
        _ratingRepo = ratingRepo;

        _lpackRepo = new LevelPackRepository(perCtx);
        _ipackRepo = new ItemPackRepository(perCtx);
        _tsourceRepo = new TrophySourceRepository(perCtx);
        _prizeRepo = new PrizeRepository(perCtx);

        // register game-related bootstrap services
        invmgr.registerDispatcher(new LobbyDispatcher(this), MsoyCodes.GAME_GROUP);
        invmgr.registerDispatcher(new AVRDispatcher(this), MsoyCodes.GAME_GROUP);

        // register to hear when the server is shutdown
        MsoyGameServer.registerShutdowner(this);
    }

    /**
     * Returns the game repository used to maintain our persistent data.
     */
    public GameRepository getGameRepository ()
    {
        return _gameRepo;
    }

    /**
     * Returns the repository used to maintain trophy to player mappings.
     */
    public TrophyRepository getTrophyRepository ()
    {
        return _trophyRepo;
    }

    /**
     * Returns an enumeration of all of the registered lobby managers.  This should only be
     * accessed on the dobjmgr thread and shouldn't be kept around across event dispatches.
     */
    public Iterator<LobbyManager> enumerateLobbyManagers ()
    {
        return _lobbies.values().iterator();
    }

    /**
     * Returns the percentiler for the specified game and score distribution. The percentiler may
     * be modified and when the lobby for the game in question is finally unloaded, the percentiler
     * will be written back out to the database.
     */
    public Percentiler getScoreDistribution (int gameId, boolean multiplayer)
    {
        return _distribs.get(multiplayer ? Math.abs(gameId) : -Math.abs(gameId));
    }

    /**
     * Resolves the item and level packs owned by the player in question for the specified game, as
     * well as trophies they have earned. This information will show up asynchronously, once the
     */
    public void resolveOwnedContent (final int gameId, final PlayerObject plobj)
    {
        // if we've already resolved content for this player, we are done
        if (plobj.isContentResolved(gameId)) {
            return;
        }

        // add our "already resolved" marker and then start resolving
        plobj.addToGameContent(new GameContentOwnership(gameId, GameData.RESOLVED_MARKER, ""));
        MsoyGameServer.invoker.postUnit(new RepositoryUnit("resolveOwnedContent") {
            public void invokePersist () throws Exception {
                // TODO: load level and item pack ownership
                _trophies = _trophyRepo.loadTrophyOwnership(gameId, plobj.getMemberId());
            }
            public void handleSuccess () {
                plobj.startTransaction();
                try {
                    addContent(GameData.LEVEL_DATA, _lpacks);
                    addContent(GameData.ITEM_DATA, _ipacks);
                    addContent(GameData.TROPHY_DATA, _trophies);
                } finally {
                    plobj.commitTransaction();
                }
            }
            protected void addContent (byte type, List<String> idents) {
                for (String ident : idents) {
                    plobj.addToGameContent(new GameContentOwnership(gameId, type, ident));
                }
            }
            protected String getFailureMessage () {
                return "Failed to resolve content [game=" + gameId + ", who=" + plobj.who() + "].";
            }

            protected List<String> _lpacks = new ArrayList<String>();
            protected List<String> _ipacks = new ArrayList<String>();
            protected List<String> _trophies;
        });
    }

    // from AVRProvider
    public void activateGame (ClientObject caller, final int gameId, final ResultListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject) caller;

        ResultListener joinListener = new AVRGameJoinListener(player.getMemberId(), listener);

        ResultListenerList list = _loadingAVRGames.get(gameId);
        if (list != null) {
            list.add(joinListener);
            return;
        }

        AVRGameManager mgr = _avrgManagers.get(gameId);
        if (mgr != null) {
            joinListener.requestProcessed(mgr);
            return;
        }

        _loadingAVRGames.put(gameId, list = new ResultListenerList());
        list.add(joinListener);

        final AVRGameManager fmgr = new AVRGameManager(gameId, _avrgRepo);
        final AVRGameObject gameObj = fmgr.createGameObject();

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("activateAVRGame") {
            public void invokePersist () throws Exception {
                if (gameId == Game.TUTORIAL_GAME_ID) {
                    _game = AVRGameManager.getTutorialGame();
                } else {
                    GameRecord gRec = _gameRepo.loadGameRecord(gameId);
                    if (gRec != null) {
                        _game = (Game) gRec.toItem();
                    }
                }
                if (_game != null) {
                    _recs = _avrgRepo.getGameState(gameId);
                }
            }

            public void handleSuccess () {
                if (_game == null) {
                    reportFailure("m.no_such_game");
                    return;
                }

                _omgr.registerObject(gameObj);
                fmgr.startup(gameObj, _game, _recs);

                _avrgManagers.put(gameId, fmgr);

                ResultListenerList list = _loadingAVRGames.remove(gameId);
                if (list != null) {
                    list.requestProcessed(fmgr);
                } else {
                    log.warning(
                        "No listeners when done activating AVRGame [gameId=" + gameId + "]");
                }
            }

            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Failed to resolve AVRGame [id=" + gameId + "].", pe);
                reportFailure(pe.getMessage());
            }

            protected void reportFailure (String reason) {
                ResultListenerList list = _loadingAVRGames.remove(gameId);
                if (list != null) {
                    list.requestFailed(reason);
                } else {
                    log.warning(
                        "No listeners when failing AVRGame [gameId=" + gameId + "]");
                }
            }

            protected Game _game;
            protected List<GameStateRecord> _recs;
        });
    }

    // from AVRProvider
    public void deactivateGame (ClientObject caller, int gameId, final ConfirmListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject) caller;
        int playerId = player.getMemberId();

        // see if we are still just resolving the game
        ResultListenerList list = _loadingAVRGames.get(gameId);
        if (list != null) {
            // yep, so just remove our associated listener, and we're done
            for (ResultListener gameListener : list) {
                if (((AVRGameJoinListener) gameListener).getPlayerId() == playerId) {
                    list.remove(gameListener);
                    listener.requestProcessed();
                    return;
                }
            }
        }

        // get the corresponding manager
        AVRGameManager mgr = _avrgManagers.get(gameId);
        if (mgr != null) {
            mgr.removePlayer(player);
            MsoyGameServer.worldClient.leaveAVRGame(playerId);

        } else {
            log.warning("Tried to deactivate AVRG without manager [gameId=" + gameId + "]");
        }

        listener.requestProcessed();
    }

    // from LobbyProvider
    public void identifyLobby (ClientObject caller, final int gameId,
                               InvocationService.ResultListener listener)
        throws InvocationException
    {
        // if we're already resolving this lobby, add this listener to the list of those interested
        // in the outcome
        ResultListenerList list = _loadingLobbies.get(gameId);
        if (list != null) {
            list.add(listener);
            return;
        }

        // if the lobby is already resolved, we're good
        LobbyManager mgr = _lobbies.get(gameId);
        if (mgr != null) {
            listener.requestProcessed(mgr.getLobbyObject().getOid());
            return;
        }

        // otherwise we need to do the resolving
        _loadingLobbies.put(gameId, list = new ResultListenerList());
        list.add(listener);

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("loadLobby") {
            public void invokePersist () throws PersistenceException {
                _content.detail = _gameRepo.loadGameDetail(gameId);
                GameRecord rec = _gameRepo.loadGameRecord(gameId, _content.detail);
                if (rec != null) {
                    _content.game = (Game)rec.toItem();
                    // load up the score distribution information for this game as well
                    _single = _ratingRepo.loadPercentile(-Math.abs(gameId));
                    _multi = _ratingRepo.loadPercentile(Math.abs(gameId));
                    // load up our level and item packs
                    for (LevelPackRecord record :
                             _lpackRepo.loadOriginalItemsBySuite(_content.game.getSuiteId())) {
                        _content.lpacks.add((LevelPack)record.toItem());
                    }
                    for (ItemPackRecord record :
                             _ipackRepo.loadOriginalItemsBySuite(_content.game.getSuiteId())) {
                        _content.ipacks.add((ItemPack)record.toItem());
                    }
                    // load up our trophy source items
                    for (TrophySourceRecord record :
                             _tsourceRepo.loadOriginalItemsBySuite(_content.game.getSuiteId())) {
                        _content.tsources.add((TrophySource)record.toItem());
                    }
                    // load up our prize items
                    for (PrizeRecord record :
                             _prizeRepo.loadOriginalItemsBySuite(_content.game.getSuiteId())) {
                        _content.prizes.add((Prize)record.toItem());
                    }
                }
            }

            public void handleSuccess () {
                if (_content.game == null) {
                    reportFailure("m.no_such_game");
                    return;
                }

                try {
                    LobbyManager lmgr = new LobbyManager(_omgr, GameGameRegistry.this);
                    lmgr.setGameContent(_content);
                    _lobbies.put(gameId, lmgr);

                    ResultListenerList list = _loadingLobbies.remove(gameId);
                    if (list != null) {
                        list.requestProcessed(lmgr.getLobbyObject().getOid());
                    }

                    // map this game's score distributions
                    _distribs.put(-Math.abs(gameId), _single == null ? new Percentiler() : _single);
                    _distribs.put(Math.abs(gameId), _multi == null ? new Percentiler() : _multi);

                } catch (Exception e) {
                    handleFailure(e);
                }
            }

            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Failed to resolve game [id=" + gameId + "].", pe);
                reportFailure(InvocationCodes.E_INTERNAL_ERROR);
            }

            protected void reportFailure (String reason) {
                ResultListenerList list = _loadingLobbies.remove(gameId);
                if (list != null) {
                    list.requestFailed(reason);
                }

                // clear out the hosting record that our world server assigned to us when it sent
                // this client our way to resolve this game
                MsoyGameServer.worldClient.stoppedHostingGame(gameId);
            }

            protected GameContent _content = new GameContent();
            protected Percentiler _single, _multi;
        });
    }

    // from LobbyProvider
    public void playNow (final ClientObject caller, final int gameId,
                         final InvocationService.ResultListener listener)
        throws InvocationException
    {
        // we need to make sure the lobby is resolved, so route through identifyLobby
        identifyLobby(caller, gameId, new InvocationService.ResultListener() {
            public void requestProcessed (Object result) {
                // now the lobby should be resolved
                LobbyManager mgr = _lobbies.get(gameId);
                if (mgr == null) {
                    log.warning("identifyLobby() returned non-failure but lobby manager " +
                                "disappeared [gameId=" + gameId + "].");
                    requestFailed(InvocationCodes.E_INTERNAL_ERROR);
                } else {
                    // if we're able to start a play now game, return zero otherwise return the
                    // lobby manager oid which will allow the client to fallback to the lobby
                    listener.requestProcessed(mgr.playNow((PlayerObject)caller) ? 0 : result);
                }
            }
            public void requestFailed (String cause) {
                listener.requestFailed(cause);
            }
        });
    }

    // from LobbyProvider
    public void joinPlayerGame (ClientObject caller, final int playerId,
                                InvocationService.ResultListener listener)
        throws InvocationException
    {
        PlayerObject player = MsoyGameServer.lookupPlayer(playerId);
        if (player == null) {
            listener.requestFailed("e.player_not_found");
            return;
        }

        // If they're not in a location, we can send that on immediately.
        int placeOid = player.getPlaceOid();
        if (placeOid == -1) {
            listener.requestProcessed(placeOid);
            return;
        }

        // Check to make sure the game that they're in is watchable
        PlaceManager plman = MsoyBaseServer.plreg.getPlaceManager(placeOid);
        if (plman == null) {
            log.warning(
                "Fetched null PlaceManager for player's current gameOid [" + placeOid + "]");
            listener.requestFailed("e.player_not_found");
            return;
        }
        MsoyGameConfig gameConfig = (MsoyGameConfig) plman.getConfig();
        MsoyMatchConfig matchConfig = (MsoyMatchConfig) gameConfig.getGameDefinition().match;
        if (matchConfig.unwatchable) {
            listener.requestFailed("e.unwatchable_game");
            return;
        }

        // Check to make sure the game that they're in is not private
        int gameId = gameConfig.getGameId();
        LobbyManager lmgr = _lobbies.get(gameId);
        if (lmgr == null) {
            log.warning("No lobby manager found for existing game! [" + gameId + "]");
            listener.requestFailed("e.player_not_found");
            return;
        }
        LobbyObject lobj = lmgr.getLobbyObject();
        for (Table table : lobj.tables) {
            if (table.gameOid == placeOid) {
                if (table.tconfig.privateTable) {
                    listener.requestFailed("e.private_game");
                    return;
                }
                break;
            }
        }

        // finally, hand off the game oid
        listener.requestProcessed(placeOid);
    }

    // from interface PresentsServer.Shutdowner
    public void shutdown ()
    {
        // shutdown our active lobbies
        for (LobbyManager lmgr : _lobbies.values().toArray(new LobbyManager[_lobbies.size()])) {
            lobbyDidShutdown(lmgr.getGame());
        }

        for (AVRGameManager amgr : _avrgManagers.values()) {
            amgr.shutdown();
        }
    }

    // from interface LobbyManager.ShutdownObserver
    public void lobbyDidShutdown (final Game game)
    {
        // destroy our record of that lobby
        _lobbies.remove(game.gameId);
        _loadingLobbies.remove(game.gameId); // just in case

        // let our world server know we're audi
        MsoyGameServer.worldClient.stoppedHostingGame(game.gameId);

        // flush any modified percentile distributions
        flushPercentiler(-Math.abs(game.gameId)); // single-player
        flushPercentiler(Math.abs(game.gameId)); // multiplayer
    }

    protected void joinAVRGame (final int playerId, final AVRGameManager mgr,
                                final ResultListener listener)
    {
        final PlayerObject player = MsoyGameServer.lookupPlayer(playerId);
        if (player == null) {
            // they left while we were resolving the game, oh well
            return;
        }

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("joinAVRGame") {
            public void invokePersist () throws Exception {
                _questRecs = _avrgRepo.getQuests(mgr.getGameId(), playerId);
                _stateRecs = _avrgRepo.getPlayerGameState(mgr.getGameId(), playerId);
            }
            public void handleSuccess () {
                mgr.addPlayer(player, _questRecs, _stateRecs);
                listener.requestProcessed(mgr.getGameObject().getOid());
            }
            public void handleFailure (Exception pe) {
                log.warning(
                    "Unable to reslve player game state [gameId=" + mgr.getGameId() +
                    ", player=" + playerId + ", error=" + pe + "]");
            }
            protected List<QuestStateRecord> _questRecs;
            protected List<PlayerGameStateRecord> _stateRecs;
        });
    }

    protected void flushPercentiler (final int gameId)
    {
        final Percentiler tiler = _distribs.remove(gameId);
        if (tiler == null || !tiler.isModified()) {
            return;
        }

        MsoyGameServer.invoker.postUnit(new Invoker.Unit("flushPercentiler") {
            public boolean invoke () {
                try {
                    _ratingRepo.updatePercentile(gameId, tiler);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to update score distribution " +
                            "[game=" + gameId + ", tiler=" + tiler + "].", pe);
                }
                return false;
            }
        });
    }

    protected final class AVRGameJoinListener
        implements ResultListener
    {
        protected AVRGameJoinListener (int player, ResultListener listener)
        {
            _listener = listener;
            _player = player;
        }

        public int getPlayerId ()
        {
            return _player;
        }

        public void requestProcessed (Object result) {
            joinAVRGame(_player, (AVRGameManager) result, _listener);
        }

        public void requestFailed (String cause) {
            _listener.requestFailed(cause);
        }

        protected final int _player;
        protected final ResultListener _listener;
    }

    /** The distributed object manager that we work with. */
    protected RootDObjectManager _omgr;

    /** Maps game id -> lobby. */
    protected IntMap<LobbyManager> _lobbies = new HashIntMap<LobbyManager>();

    /** Maps game id -> a mapping of various percentile distributions. */
    protected IntMap<Percentiler> _distribs = new HashIntMap<Percentiler>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected IntMap<ResultListenerList> _loadingLobbies = new HashIntMap<ResultListenerList>();

    /** Maps game id -> manager for AVR games. */
    protected IntMap<AVRGameManager> _avrgManagers = new HashIntMap<AVRGameManager>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected IntMap<ResultListenerList> _loadingAVRGames = new HashIntMap<ResultListenerList>();

    // various and sundry repositories for loading persistent data
    protected GameRepository _gameRepo;
    protected AVRGameRepository _avrgRepo;
    protected RatingRepository _ratingRepo;
    protected TrophyRepository _trophyRepo;
    protected LevelPackRepository _lpackRepo;
    protected ItemPackRepository _ipackRepo;
    protected TrophySourceRepository _tsourceRepo;
    protected PrizeRepository _prizeRepo;
}
