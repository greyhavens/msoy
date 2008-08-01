//
// $Id$

package com.threerings.msoy.avrg.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.samskivert.jdbc.WriteOnlyUnit;

import com.threerings.parlor.server.PlayManager;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.bureau.server.BureauRegistry;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceManagerDelegate;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.QuestState;
import com.threerings.msoy.game.server.AgentTraceDelegate;
import com.threerings.msoy.game.server.GameWatcherManager;
import com.threerings.msoy.game.server.PlayerLocator;
import com.threerings.msoy.game.server.GameWatcherManager.Observer;

import com.threerings.msoy.avrg.data.AVRGameAgentObject;
import com.threerings.msoy.avrg.data.AVRGameConfig;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.PlayerLocation;
import com.threerings.msoy.avrg.data.SceneInfo;
import com.threerings.msoy.avrg.server.AVRGameDispatcher;
import com.threerings.msoy.avrg.server.persist.AVRGameRepository;
import com.threerings.msoy.avrg.server.persist.GameStateRecord;
import com.threerings.msoy.avrg.server.persist.PlayerGameStateRecord;

import com.whirled.bureau.data.BureauTypes;
import com.whirled.game.server.WhirledGameManager;

import static com.threerings.msoy.Log.log;

/**
 * Manages an AVR game on the server.
 */
@EventThread
public class AVRGameManager extends PlaceManager
    implements AVRGameProvider, PlayManager
{
    /** Observes our shutdown function call. */
    public interface LifecycleObserver
    {
        /** Informs the observer the AVRG manager is ready for operation. */
        void avrGameReady (AVRGameManager mgr);

        /** Informs the observer a shutdown has happened. */
        void avrGameDidShutdown (AVRGameManager mgr);
    }

    public void setLifecycleObserver (LifecycleObserver obs)
    {
        _lifecycleObserver = obs;
    }

    public int getGameId ()
    {
        return _gameId;
    }

    public AVRGameObject getGameObject ()
    {
        return _gameObj;
    }

    public AVRGameAgentObject getGameAgentObject ()
    {
        return _gameAgentObj;
    }

    /**
     * Called with our hosted game's data once it's finished loading from the database.
     */
    public void initializeState (List<GameStateRecord> stateRecords)
    {
        _gameObj.startTransaction();
        try {
            for (GameStateRecord rec : stateRecords) {
                _gameObj.addToState(rec.toEntry());
            }
        } finally {
            _gameObj.commitTransaction();
        }
    }

    @Override // from PlaceManagerDelegate
    public void addDelegate (PlaceManagerDelegate delegate)
    {
        super.addDelegate(delegate);

        if (delegate instanceof QuestDelegate) {
            _questDelegate = (QuestDelegate) delegate;
        }
    }

    // from PlayManager
    public boolean isPlayer (ClientObject client)
    {
        return client != null && (client instanceof PlayerObject) &&
            _gameObj.occupants.contains(client.getOid());
    }

    // from PlayManager
    public boolean isAgent (ClientObject caller)
    {
        return _gameAgentObj != null && _gameAgentObj.clientOid == caller.getOid();
    }

    // from PlayManager
    public BodyObject checkWritePermission (ClientObject caller, int playerId)
    {
        if (isAgent(caller)) {
            return (playerId != 0) ? _locator.lookupPlayer(playerId) : null;
        }
        return (playerId == 0) ? (BodyObject) caller : null;
    }

    /**
     * Called privately by the ThaneAVRGameController when anything in the agent's code domain
     * causes a line of debug or error tracing.
     */
    public void agentTrace (ClientObject caller, String trace)
    {
        _traceDelegate.recordAgentTrace(trace);
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new AVRGameObject();
    }

    @Override
    protected void didStartup ()
    {
        AVRGameConfig cfg = (AVRGameConfig)_config;

        _gameId = cfg.getGameId();

        _gameObj = (AVRGameObject)_plobj;
        _gameObj.setAvrgService(_invmgr.registerDispatcher(new AVRGameDispatcher(this)));
    }

    public void startAgent ()
    {
        AVRGameConfig cfg = (AVRGameConfig)_config;
        MsoyGameDefinition def = (MsoyGameDefinition) cfg.getGameDefinition();

        _gameAgentObj = createGameAgentObject(_gameId, def);
        if (_gameAgentObj == null) {
            // if there is no agent, force a call to agentReady
            _lifecycleObserver.avrGameReady(this);
            return;
        }

        // else ask the bureau to start it and wait for its initialization
        _gameAgentObj.gameOid = _gameObj.getOid();
        _breg.startAgent(_gameAgentObj);
    }

    /**
     * The game has changed while we're hosting it; update the media, the client will reload.
     */
    public void updateGame (Game game)
    {
        _gameObj.setGameMedia(game.gameMedia);
    }

    // from AVRGameProvider
    public void startQuest (ClientObject caller, final String questId, final String status,
                            InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        _questDelegate.startQuest(caller, questId, status, listener);
    }

    // from AVRGameProvider
    public void updateQuest (ClientObject caller, final String questId, final int step,
                             final String status, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        _questDelegate.updateQuest(caller, questId, step, status, listener);
    }

    // from AVRGameProvider
    public void completeQuest (ClientObject caller, final String questId, final float payoutLevel,
                               InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        _questDelegate.completeQuest(caller, questId, payoutLevel, listener);
    }

    // from AVRGameProvider
    public void cancelQuest (ClientObject caller, final String questId,
                             InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        _questDelegate.cancelQuest(caller, questId, listener);
    }

    // from AVRGameProvider
    public void setProperty (ClientObject caller, String key, byte[] value, boolean persistent,
                             InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        if (key == null) {
            log.warning("Can't set null-keyed property [game=" + where() +
                        ", who=" + caller.who() + "]");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        GameState entry = new GameState(key, value, persistent);

        // TODO: verify that the memory does not exceed legal size

        entry.modified = true;
        if (_gameObj.state.contains(entry)) {
            _gameObj.updateState(entry);
        } else if (value != null) {
            _gameObj.addToState(entry);
        }
        listener.requestProcessed();
    }

    // from AVRGameProvider
    public void deleteProperty (ClientObject caller, String key,
                                InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        setProperty(caller, key, null, true, listener);
    }

    // from AVRGameProvider
    public void setPlayerProperty (ClientObject caller, String key, byte[] value,
                                   boolean persistent, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject) caller;
        if (key == null) {
            log.warning("Can't set null-keyed player property [game=" + where() +
                        ", who=" + player.who() + "]");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        GameState entry = new GameState(key, value, persistent);

        // TODO: verify that the memory does not exceed legal size

        entry.modified = true;
        if (player.gameState.contains(entry)) {
            player.updateGameState(entry);
        } else if (value != null) {
            player.addToGameState(entry);
        }
        listener.requestProcessed();
    }

    // from AVRGameProvider
    public void deletePlayerProperty (ClientObject caller, String key,
                                      InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        setPlayerProperty(caller, key, null, true, listener);
    }

    // from AVRGameProvider
    public void sendMessage (ClientObject caller, String msg, Object data, int playerId,
                             InvocationService.InvocationListener listener)
        throws InvocationException
    {
        if (playerId == 0) {
            // send to everyone
            _gameObj.postMessage(AVRGameObject.USER_MESSAGE, msg, data);
            return;
        }

        // send to a specific player
        PlayerObject player = _locator.lookupPlayer(playerId);
        if (player != null) {
            player.postMessage(
                AVRGameObject.USER_MESSAGE + ":" + _gameObj.getOid(),
                new Object[] { msg, data });
        }
    }

    // from AVRGameProvider
    public void setTicker (ClientObject caller, String tickerName, int msOfDelay,
                           InvocationService.InvocationListener listener)
        throws InvocationException
    {
        Ticker t;
        if (msOfDelay >= MIN_TICKER_DELAY) {
            if (_tickers != null) {
                t = _tickers.get(tickerName);
            } else {
                _tickers = new HashMap<String, Ticker>();
                t = null;
            }

            if (t == null) {
                if (_tickers.size() >= MAX_TICKERS) {
                    throw new InvocationException(InvocationCodes.ACCESS_DENIED);
                }
                t = new Ticker(tickerName, _gameObj);
                _tickers.put(tickerName, t);
            }
            t.start(msOfDelay);

        } else if (msOfDelay <= 0) {
            if (_tickers != null) {
                t = _tickers.remove(tickerName);
                if (t != null) {
                    t.stop();
                }
            }

        } else {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
    }

    /**
     * Called privately by the ThaneAVRGameController when an agent's code is all set to go
     * and the AVRG can startup.
     */
    public void agentReady (ClientObject caller)
    {
        log.info("AVRG Agent ready for " + caller);
        _lifecycleObserver.avrGameReady(this);
    }

    /**
     * This method is called when the agent completes the subscription of a scene's RoomObject.
     * TODO: wire up
     */
    public void roomSubscriptionComplete (int sceneId)
    {
        IntSet removes = new ArrayIntSet();

        for (IntIntMap.IntIntEntry entry : _pendingMoves.entrySet()) {
            if (entry.getValue() == sceneId) {
                int memberId = entry.getKey();
                removes.add(memberId);

                PlayerLocation loc = new PlayerLocation(memberId, sceneId);
                if (_gameObj.playerLocs.contains(loc)) {
                    _gameObj.updatePlayerLocs(loc);

                } else {
                    _gameObj.addToPlayerLocs(loc);
                }
            }
        }

        for (int memberId : removes) {
            _pendingMoves.remove(memberId);
        }
    }

    @Override
    protected void didShutdown ()
    {
        stopTickers();

        if (_gameAgentObj != null) {
            _breg.destroyAgent(_gameAgentObj);
        }

        // identify any modified memory records for flushing to the database
        final List<GameStateRecord> recs = new ArrayList<GameStateRecord>();
        for (GameState entry : _gameObj.state) {
            if (entry.persistent && entry.modified) {
                recs.add(new GameStateRecord(_gameId, entry));
            }
        }

        _invoker.postUnit(new WriteOnlyUnit("shutdown") {
            public void invokePersist () throws Exception {
                for (GameStateRecord rec : recs) {
                    _repo.storeState(rec);
                }
            }
        });

        if (_lifecycleObserver != null) {
            _lifecycleObserver.avrGameDidShutdown(this);
        }
    }

    @Override
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        PlayerObject player = (PlayerObject) _omgr.getObject(bodyOid);
        _watchmgr.addWatch(player.getMemberId(), _observer);

        // TODO: make sure the AVRG does not initialize for this player until Room Subscription
    }

    @Override
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        PlayerObject player = (PlayerObject) _omgr.getObject(bodyOid);

        int memberId = player.getMemberId();
        // stop watching this player's movements
        _watchmgr.clearWatch(memberId);

        // clear out any pending move information
        _pendingMoves.remove(memberId);

        flushPlayerGameState(player);
    }

    @Override
    protected long idleUnloadPeriod ()
    {
        return IDLE_UNLOAD_PERIOD;
    }

    protected void playerEnteredScene (int memberId, int sceneId, String hostname, int port)
    {
        log.debug(
            "Player entered scene [memberId=" + memberId + ", sceneId=" + sceneId +
            ", hostname=" + hostname + ", port=" + port + "]");

        // TODO: this is incomplete, we need to keep a set of rooms that we've added to 'scenes'
        // TODO: but which have not yet completed subscription; we should obviously not add
        // TODO: players to _pendingMoves when they're moving into rooms we already know of

        // until room subscription completes, just make a note that this player is in this scene
        _pendingMoves.put(memberId, sceneId);

        SceneInfo info = new SceneInfo(sceneId, hostname, port);
        if (_gameAgentObj.scenes.contains(info)) {
            // there might be old scene data lingering, with the scene now being hosted elsewhere
            if (!info.equals(_gameAgentObj.scenes.get(sceneId))) {
                _gameAgentObj.updateScenes(info);
            }

        } else {
            _gameAgentObj.addToScenes(info);
        }

        // TODO: this is only for debugging
        roomSubscriptionComplete(sceneId);
    }

    protected void flushPlayerGameState (final PlayerObject player)
    {
        // find any modified memory records
        final List<PlayerGameStateRecord> recs = new ArrayList<PlayerGameStateRecord>();

        // unless we're a guest
        if (!MemberName.isGuest(player.getMemberId())) {
            for (GameState entry : player.gameState) {
                if (entry.persistent && entry.modified) {
                    recs.add(new PlayerGameStateRecord(_gameId, player.getMemberId(), entry));
                }
            }
        }

        // then clear out the state associated with this game
        player.setQuestState(new DSet<QuestState>());
        player.setGameState(new DSet<GameState>());

        // if we didn't find any modified records we're done
        if (recs.size() == 0) {
            return;
        }

        // else flush them to the database
        _invoker.postUnit(new WriteOnlyUnit("removePlayer(" + player + ")") {
            public void invokePersist () throws Exception {
                for (PlayerGameStateRecord rec : recs) {
                    _repo.storePlayerState(rec);
                }
            }
        });
    }

    /**
     * Stop and clear all tickers.
     */
    protected void stopTickers ()
    {
        if (_tickers != null) {
            for (Ticker ticker : _tickers.values()) {
                ticker.stop();
            }
            _tickers = null;
        }
    }

    /**
     * Reports the id/name of this game for use in log messages.
     */
    @Override
    public String where ()
    {
        return String.valueOf(_gameId);
    }

    protected AVRGameAgentObject createGameAgentObject (int gameId, MsoyGameDefinition def)
    {
        String code = def.getServerMediaPath(gameId);
        if (StringUtil.isBlank(code)) {
            return null;
        }

        AVRGameAgentObject agent = new AVRGameAgentObject();
        agent.bureauId = BureauTypes.GAME_BUREAU_ID_PREFIX + gameId;
        agent.bureauType = BureauTypes.THANE_BUREAU_TYPE;
        agent.code = code;
        if (StringUtil.isBlank(def.server)) {
            agent.className = WhirledGameManager.DEFAULT_SERVER_CLASS;
        } else {
            agent.className = def.server;
        }
        return agent;
    }

    /**
     * A timer that fires message events to an AVRG. This is a precise copy of the same class
     * in WhirledGameManager. Perhaps one day we can avoid this duplication.
     */
    protected static class Ticker
    {
        /**
         * Create a Ticker.
         */
        public Ticker (String name, AVRGameObject gameObj)
        {
            _name = name;
            // once we are constructed, we want to avoid calling methods on dobjs.
            _oid = gameObj.getOid();
            _omgr = gameObj.getManager();
        }

        public void start (int msOfDelay)
        {
            _value = 0;
            _interval.schedule(0, msOfDelay);
        }

        public void stop ()
        {
            _interval.cancel();
        }

        /**
         * The interval that does our work. Note well that this is not a 'safe' interval that
         * operates using a RunQueue.  This interval instead does something that we happen to know
         * is safe for any thread: posting an event to the dobj manager.  If we were using a
         * RunQueue it would be the same event queue and we would be posted there, wait our turn,
         * and then do the same thing: post this event. We just expedite the process.
         */
        protected Interval _interval = new Interval() {
            public void expired () {
                _omgr.postEvent(new MessageEvent(
                    _oid, AVRGameObject.TICKER, new Object[] { _name, _value++ }));
            }
        };

        protected int _oid;
        protected DObjectManager _omgr;
        protected String _name;
        protected int _value;
    } // End: static class Ticker

    protected Observer _observer = new Observer() {
        public void memberMoved (int memberId, int sceneId, String hostname, int port) {
            playerEnteredScene(memberId, sceneId, hostname, port);
        }
    };

    /** The gameId of this particular AVRG. */
    protected int _gameId;

    /** The distributed object that both clients and the agent sees. */
    protected AVRGameObject _gameObj;

    /** The distributed object that only our agent sees. */
    protected AVRGameAgentObject _gameAgentObj;

    /** Tracks player that've moved to a scene not yet subscribed to by the agent. */
    protected IntIntMap _pendingMoves = new IntIntMap();

    /** The map of tickers, lazy-initialized. */
    protected HashMap<String, Ticker> _tickers;

    /** Observer of our shutdown. */
    protected LifecycleObserver _lifecycleObserver;

    /** The delegate that handles quest completion and coin payouts. */
    protected QuestDelegate _questDelegate;

    /** A delegate that handles agent traces.. */
    protected AgentTraceDelegate _traceDelegate;

    // our dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected InvocationManager _invmgr;
    @Inject protected GameWatcherManager _watchmgr;
    @Inject protected AVRGameRepository _repo;
    @Inject protected BureauRegistry _breg;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected PlayerLocator _locator;

    /** The minimum delay a ticker can have. */
    protected static final int MIN_TICKER_DELAY = 50;

    /** The maximum number of tickers allowed at one time. */
    protected static final int MAX_TICKERS = 3;

    /** idle time before shutting down the manager. */
    protected static final long IDLE_UNLOAD_PERIOD = 60 * 1000L; // in ms
}
