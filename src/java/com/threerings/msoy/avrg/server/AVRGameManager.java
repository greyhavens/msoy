//
// $Id$

package com.threerings.msoy.avrg.server;

import java.util.HashMap;
import java.util.Iterator;

import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.samskivert.util.IntMap.IntEntry;

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

import com.threerings.msoy.item.data.all.Game;

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
import com.whirled.bureau.data.BureauTypes;
import com.whirled.game.server.WhirledGameManager;
import com.whirled.game.server.WhirledGameMessageDispatcher;
import com.whirled.game.server.WhirledGameMessageHandler;

import static com.threerings.msoy.Log.log;

/**
 * Manages an AVR game on the server.
 */
@EventThread
public class AVRGameManager extends PlaceManager
    implements AVRGameProvider, PlayManager
{
    /** The magic player id constant for the server agent used when sending private messages. */
    public static final int SERVER_AGENT = Integer.MIN_VALUE;

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

    @Override // from PlaceManager
    public void addDelegate (PlaceManagerDelegate delegate)
    {
        super.addDelegate(delegate);

        if (delegate instanceof QuestDelegate) {
            _questDelegate = (QuestDelegate) delegate;
        }

        if (delegate instanceof AgentTraceDelegate) {
            _traceDelegate = (AgentTraceDelegate) delegate;
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
    public void agentTrace (ClientObject caller, String[] traces)
    {
        _traceDelegate.recordAgentTrace(traces);
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
        _gameObj.setMessageService(_invmgr.registerDispatcher(new WhirledGameMessageDispatcher(
            new WhirledGameMessageHandler(_gameObj) {
                @Override protected ClientObject getAudienceMember (int id)
                    throws InvocationException {
                    ClientObject target = null;
                    if (id == SERVER_AGENT) {
                        if (_gameAgentObj != null && _gameAgentObj.clientOid != 0) {
                            target = (ClientObject)_omgr.getObject(_gameAgentObj.clientOid);
                        }
                    } else {
                        target = _locator.lookupPlayer(id);
                    }
                    if (target == null) {
                        throw new InvocationException("m.player_not_around");
                    }
                    return target;
                }

                @Override protected void validateSender (ClientObject caller)
                    throws InvocationException {
                    validateUser(caller);
                }

                @Override protected boolean isAgent (ClientObject caller) {
                    return AVRGameManager.this.isAgent(caller);
                }

                @Override protected int resolvePlayerId (ClientObject caller) {
                    return ((PlayerObject)caller).getMemberId();
                }
            })));
        
        _sceneCheck = new Interval(_omgr) {
            public void expired () {
                checkScenes();
            }
        };
        _sceneCheck.schedule(SCENE_CHECK_PERIOD, true);
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
// TODO
//        _gameObj.setGameMedia(game.gameMedia);
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
    public void roomSubscriptionComplete (ClientObject caller, int sceneId)
    {
        if (caller.getOid() != _gameAgentObj.clientOid) {
            log.warning("Unknown client completing room subscription?", "caller", caller);
            return;
        }
        
        roomSubscriptionComplete(sceneId);
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
     */
    public void roomSubscriptionComplete (int sceneId)
    {
        Scene scene = _scenes.get(sceneId);
        
        if (scene == null) {
            log.warning("Subscription completed to removed scene", "sceneId", sceneId);
            return;
        }
        
        scene.subscribed = true;
        
        for (int memberId : scene.players) {
            postPlayerMove(memberId, scene.sceneId);
        }
    }
    
    /**
     * Post the given member's movement to the game object for all to see.
     */
    protected void postPlayerMove (int memberId, int sceneId)
    {
        PlayerLocation loc = new PlayerLocation(memberId, sceneId);
        if (_gameObj.playerLocs.contains(loc)) {
            _gameObj.updatePlayerLocs(loc);

        } else {
            _gameObj.addToPlayerLocs(loc);
        }
    }

    @Override
    protected void didShutdown ()
    {
        stopTickers();

        if (_gameAgentObj != null) {
            _breg.destroyAgent(_gameAgentObj);
        }

        if (_lifecycleObserver != null) {
            _lifecycleObserver.avrGameDidShutdown(this);
        }
        
        _invmgr.clearDispatcher(_gameObj.avrgService);
        _invmgr.clearDispatcher(_gameObj.messageService);
        
        _sceneCheck.cancel();
    }

    @Override
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        PlayerObject player = (PlayerObject) _omgr.getObject(bodyOid);
        _watchmgr.addWatch(player.getMemberId(), _observer);
    }

    @Override
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        PlayerObject player = (PlayerObject) _omgr.getObject(bodyOid);

        int memberId = player.getMemberId();
        // stop watching this player's movements
        _watchmgr.clearWatch(memberId);

        // clear out from our internal record
        Scene scene = _players.remove(memberId);
        if (scene == null) {
            log.warning("Leaving body has no scene", "memberId", memberId);
        } else {
            scene.removePlayer(memberId);

            // remove from player locations
            if (_gameObj.playerLocs.containsKey(memberId)) {
                _gameObj.removeFromPlayerLocs(memberId);
            
            } else if (scene.subscribed) {
                log.warning("Player leaving subscribed scene not in playerLocs?", 
                    "scene", scene, "memberId", memberId);
            }
        }

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

        SceneInfo info = new SceneInfo(sceneId, hostname, port);
        SceneInfo existing = _gameAgentObj.scenes.get(info.getKey());
        if (existing != null) {
            if (!info.equals(existing)) {
                // this should not happen in normal operation, but is feasible if a server goes 
                // down abruptly
                log.warning("Updating stale SceneInfo", "old", existing, "new", info);
                _gameAgentObj.updateScenes(info);
            }

        } else {
            _gameAgentObj.addToScenes(info);
        }

        // Update our internal records
        Scene scene = _scenes.get(sceneId);
        Scene oldScene = _players.get(memberId);
        if (scene == null) {
            _scenes.put(sceneId, scene = new Scene(sceneId));
        }
        _players.put(memberId, scene);
        scene.addPlayer(memberId);
        if (oldScene != null) {
            oldScene.removePlayer(memberId);
        }

        // Expose the transfer to dobj (or else wait until agent calls roomSubscriptionComplete)
        if (scene.subscribed || _gameAgentObj==null) {
            postPlayerMove(memberId, sceneId);
        }
    }

    protected void flushPlayerGameState (final PlayerObject player)
    {
        // then clear out the state associated with this game
        player.setQuestState(new DSet<QuestState>());
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
        agent.gameId = gameId;
        agent.code = code;
        if (StringUtil.isBlank(def.server)) {
            agent.className = WhirledGameManager.DEFAULT_SERVER_CLASS;
        } else {
            agent.className = def.server;
        }
        return agent;
    }
    
    /** 
     * Eliminate any scenes that have been empty for a while. This is to prevent a large buildup
     * for games that visit a lot of rooms. 
     */
    protected void checkScenes ()
    {
        long now = System.currentTimeMillis();
        Iterator<IntEntry<Scene>> iter = _scenes.intEntrySet().iterator();
        while (iter.hasNext()) {
            Scene scene = iter.next().getValue();
            if (scene.shouldFlush(now)) {
                if (_gameAgentObj.scenes.containsKey(scene.sceneId)) {
                    _gameAgentObj.removeFromScenes(scene.sceneId);

                } else {
                    log.warning("Flushing scene not found in agent", "scene", scene);
                }
                iter.remove();
            }
        }
    }

    /**
     * Throws an InvocationException if the caller is not allowed.
     */
    protected void validateUser (ClientObject caller)
        throws InvocationException
    {
        if (!isPlayer(caller) && !isAgent(caller)) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
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

    /**
     * Data for tracking what players are in a scene, so that we know when to flush it.
     */
    protected static class Scene
    {
        public int sceneId;
        public long modTime;
        public boolean subscribed;
        public ArrayIntSet players = new ArrayIntSet();
        
        public Scene (int sceneId)
        {
            this.sceneId = sceneId;
            touch();
        }

        /** Sets the last modification time to the current time. */
        public void touch ()
        {
            modTime = System.currentTimeMillis(); 
        }
        
        /** Test if the scene has been empty for a while. */
        public boolean shouldFlush (long now)
        {
            if (players.size() > 0) {
                return false;
            }
            
            return (now - modTime > SCENE_IDLE_UNLOAD_PERIOD);
        }
        
        /** Add a player to the scene, automatically calling {@link #touch} as well. */
        public void addPlayer (int id)
        {
            if (!players.add(id)) {
                log.warning("Player added to scene twice", "memberId", id, "sceneId", sceneId);
            }
            touch();
        }
        
        /** Remove a player from the scene, automatically calling {@link #touch} as well. */
        public void removePlayer (int id)
        {
            if (!players.remove(id)) {
                log.warning("Player not found to remove", "memberId", id, "sceneId", sceneId);
            }
            touch();
        }
        
        public String toString ()
        {
            StringBuilder buf = new StringBuilder();
            buf.append("AVRGameManager.Scene");
            StringUtil.fieldsToString(buf, this);
            return buf.toString();
        }
    }

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

    /** The map of tickers, lazy-initialized. */
    protected HashMap<String, Ticker> _tickers;

    /** The map of scenes by scene id, one to one. */
    protected HashIntMap<Scene> _scenes = new HashIntMap<Scene>();

    /** The map of player ids to scenes, many to one. */
    protected HashIntMap<Scene> _players = new HashIntMap<Scene>();
    
    /** Interval to run our scene checker. */
    protected Interval _sceneCheck;

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
    protected static final long IDLE_UNLOAD_PERIOD = 5 * 60 * 1000L; // in ms
    
    /** Minimum time a scene must remain idle before unloading. */
    protected static final long SCENE_IDLE_UNLOAD_PERIOD = 60 * 1000;
    
    /** Time between checks to flush idle scenes. */
    protected static final long SCENE_CHECK_PERIOD = 60 * 1000;
}
