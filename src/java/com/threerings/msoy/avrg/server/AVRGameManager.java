//
// $Id$

package com.threerings.msoy.avrg.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.inject.Inject;

import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.bureau.server.BureauRegistry;
import com.threerings.crowd.data.OccupantInfo;

import com.threerings.whirled.data.ScenePlace;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.QuestState;
import com.threerings.msoy.game.server.GameContent;
import com.threerings.msoy.game.server.GameWatcherManager;
import com.threerings.msoy.game.server.WorldServerClient;
import com.threerings.msoy.game.server.GameWatcherManager.Observer;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.avrg.data.AVRGameAgentObject;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.PlayerLocation;
import com.threerings.msoy.avrg.data.SceneInfo;
import com.threerings.msoy.avrg.server.AVRGameDispatcher;
import com.threerings.msoy.avrg.server.persist.AVRGameRepository;
import com.threerings.msoy.avrg.server.persist.GameStateRecord;
import com.threerings.msoy.avrg.server.persist.PlayerGameStateRecord;
import com.threerings.msoy.avrg.server.persist.QuestLogSummaryRecord;
import com.threerings.msoy.avrg.server.persist.QuestStateRecord;

import com.whirled.bureau.data.BureauTypes;
import com.whirled.game.server.WhirledGameManager;

import static com.threerings.msoy.Log.log;

/**
 * Manages an AVR game on the server.
 */
@EventThread
public class AVRGameManager
    implements AVRGameProvider, OidListListener, AVRGameObject.SubscriberListener
{
    /** Observes our shutdown function call. */
    public interface ShutdownObserver
    {
        /** Informs the observer a shutdown has happened. */
        void avrGameDidShutdown (final Game game);
    }
    
    public void setShutdownObserver (ShutdownObserver obs)
    {
        _shutdownObserver = obs;
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
     * @param gameAgentObj
     */
    public void startup (
        int gameId, GameContent content, MsoyGameDefinition def, 
        List<GameStateRecord> stateRecords)
    {
        _gameId = gameId;
        _content = content;

        _gameObj = new AVRGameObject();
        _gameObj.subscriberListener = this;
        _omgr.registerObject(_gameObj);
        
        _gameAgentObj = createGameAgentObject(content.game, def);
        _gameAgentObj.gameOid = _gameObj.getOid();
        _breg.startAgent(_gameAgentObj);
        
        // listen for gameObj.playerOids removals
        _gameObj.addListener(this);

        _gameObj.startTransaction();
        _gameObj.setAvrgService(_invmgr.registerDispatcher(new AVRGameDispatcher(this)));
        _gameObj.setGameMedia(_content.game.gameMedia);
        try {
            for (GameStateRecord rec : stateRecords) {
                _gameObj.addToState(rec.toEntry());
            }
        } finally {
            _gameObj.commitTransaction();
        }

        _shutdownCheck = new Interval (_omgr) {
            public void expired () {
                checkForShutdown();
            }
        };
    }

    //  from AVRGameObject.SubscriberListener
    public void subscriberCountChanged (AVRGameObject target)
    {
        if (_shutdown) {
            return;
        }

        int agents = _gameAgentObj != null ? 1 : 0;
        if (target.getSubscriberCount() <= agents) {
            log.info("Scheduling shutdown check", "subs", _gameObj.getSubscriberCount());
            _shutdownCheck.schedule(IDLE_UNLOAD_PERIOD);
            
        } else {
            log.info("Cancelling shutdown check", "subs", _gameObj.getSubscriberCount());
            _shutdownCheck.cancel();            
        }
    }
    
    /**
     * The game has changed while we're hosting it; update the media, the client will reload.
     */
    public void updateGame (Game game)
    {
        _gameObj.setGameMedia(game.gameMedia);
    }

    /**
     * Called when we're going down, clean up and flush accumulated data to store.
     */
    public void shutdown ()
    {
        if (_shutdown) {
            return;
        }

        _shutdown = true;

        log.info("Shutting down avrg", "gameId", _gameId);
        
        _shutdownCheck.cancel();
        
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

        final int totalMins = Math.max(1, Math.round(getTotalTrackedSeconds() / 60f));
        _invoker.postUnit(new WriteOnlyUnit("shutdown") {
            public void invokePersist () throws Exception {
                for (GameStateRecord rec : recs) {
                    _repo.storeState(rec);
                }
                _repo.noteUnawardedTime(_gameId, totalMins);
            }
        });
        
        if (_shutdownObserver != null) {
            _shutdownObserver.avrGameDidShutdown(_content.game);
        }
    }

    // from interface OidListListener
    public void objectAdded (ObjectAddedEvent event)
    {
        // no special action
    }

    // from interface OidListListener
    public void objectRemoved (ObjectRemovedEvent event)
    {
        if (event.getName().equals(AVRGameObject.PLAYER_OIDS)) {
            int playerOid = event.getOid();
            if (_gameObj.players.containsKey(playerOid)) {
                _gameObj.removeFromPlayers(playerOid);
            } else {
                log.warning("Player removed from OidList without a corresponding DSet entry " +
                            "[gameId=" + _gameId + ", oid=" + event.getOid() + "]");
                return;
            }

            Player player = _players.remove(playerOid);
            if (player == null) {
                log.warning("Eek, unregistered player vanished from OidList [gameId=" + _gameId +
                            "playerOid=" + playerOid + "]");
                return;
            }

            int memberId = player.playerObject.getMemberId();
            // stop watching this player's movements
            _watchmgr.clearWatch(memberId);

            // clear out any pending move information
            _pendingMoves.remove(memberId);

            int playTime = player.getPlayTime(now());
            String tracker = player.playerObject.referral.tracker;
            _eventLog.avrgLeft(memberId, _gameId, playTime, _gameObj.players.size(), tracker);

            _totalTrackedSeconds += playTime;
            flushPlayerGameState(player.playerObject);
            _worldClient.updatePlayer(memberId, null);
        }
    }

    // from AVRGameProvider
    public void startQuest (ClientObject caller, final String questId, final String status,
                            InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject) caller;
        if (questId == null) {
            log.warning("Received startQuest() request with null questId [game=" + where() +
                        ", who=" + player.who() + "]");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        if (player.questState.containsKey(questId)) {
            listener.requestProcessed(); // silently ignore
            return;
        }

        final int sceneId = ScenePlace.getSceneId(player);
        _invoker.postUnit(new PersistingUnit("startQuest(" + questId + ")", listener) {
            public void invokePersistent () throws Exception {
                if (!MemberName.isGuest(player.getMemberId())) {
                    _repo.setQuestState(_gameId, player.getMemberId(), questId,
                                        QuestState.STEP_FIRST, status, sceneId);
                }
            }
            public void handleSuccess () {
                player.addToQuestState(
                    new QuestState(questId, QuestState.STEP_FIRST, status, sceneId));
                reportRequestProcessed();
            }
        });
    }

    // from AVRGameProvider
    public void updateQuest (ClientObject caller, final String questId, final int step,
                             final String status, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject) caller;

        QuestState oldState = player.questState.get(questId);
        if (oldState == null) {
            log.warning("Requested to update quest to which member was not subscribed " +
                        "[game=" + where() + ", quest=" + questId + ", who=" + player.who() + "].");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        final int sceneId = ScenePlace.getSceneId(player);

        _invoker.postUnit(new PersistingUnit("updateQuest", listener) {
            public void invokePersistent () throws Exception {
                if (!MemberName.isGuest(player.getMemberId())) {
                    _repo.setQuestState(_gameId, player.getMemberId(), questId, step,
                                        status, sceneId);
                }
            }
            public void handleSuccess () {
                player.updateQuestState(new QuestState(questId, step, status, sceneId));
                reportRequestProcessed();
            }
        });
    }

    // from AVRGameProvider
    public void completeQuest (ClientObject caller, final String questId, final float payoutLevel,
                               InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject) caller;
        final QuestState oldState = player.questState.get(questId);

        // very little is done for guests
        if (MemberName.isGuest(player.getMemberId())) {
            if (oldState != null) {
                player.removeFromQuestState(questId);
            }
            listener.requestProcessed();
            return;
        }

        // sanity check
        if (payoutLevel < 0 || payoutLevel > 1) {
            log.warning("Invalid payout in completeQuest() [game=" + where() + ", quest=" + questId +
                        ", payout=" + payoutLevel + ", caller=" + player.who() + "].");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        final int flowPerHour = RuntimeConfig.server.hourlyGameFlowRate;
        final int recalcMins = RuntimeConfig.server.payoutFactorReassessment;

        // note how much player time has accumulated since our last payout
        int playerSecs = getTotalTrackedSeconds();
        _totalTrackedSeconds -= playerSecs;
        final int playerMins = Math.round(playerSecs / 60f);

        // the tutorial pays out no flow; normal games depend on accumulated play time
        int payoutFactor;
        if (_gameId == Game.TUTORIAL_GAME_ID) {
            payoutFactor = 0;
        } else {
            // if we've yet to accumulate enough data for a calculation, guesstimate 5 mins
            payoutFactor = (_content.detail.payoutFactor == 0) ?
                (5 * flowPerHour/60) : _content.detail.payoutFactor;
        }

        // compute our quest payout; as a sanity check, cap it at one hour of payout
        int rawPayout = Math.round(payoutFactor * payoutLevel);
        final int payout = Math.min(flowPerHour, rawPayout);
        if (payout != rawPayout) {
            log.warning("Capped AVRG payout at one hour [game=" + _gameId +
                        ", factor=" + payoutFactor + ", level=" + payoutLevel +
                        ", wanted=" + rawPayout + ", got=" + payout + "].");
        }

        final int newFlowToNextRecalc;
        if (_content.detail == null) { // this is null if we're in the tutorial
            newFlowToNextRecalc = 0;
        } else {
            // note that we've used up some of our flow budget (this same adjustment will be
            // recorded to the database in noteGamePlayed())
            _content.detail.flowToNextRecalc -= payout;

            // if this payout consumed the remainder of our awardable flow, queue a factor recalc
            if (_content.detail.flowToNextRecalc <= 0) {
                newFlowToNextRecalc = flowPerHour * recalcMins + _content.detail.flowToNextRecalc;
                // update our in memory record immediately so that we don't encounter funny
                // business if another quest payout is done while we're writing this to the
                // database
                _content.detail.flowToNextRecalc = newFlowToNextRecalc;
            } else {
                newFlowToNextRecalc = 0;
            }
        }

        _invoker.postUnit(new PersistingUnit("completeQuest", listener) {
            public void invokePersistent () throws Exception {
                // award the flow for this quest
                if (payout > 0) {
                    _memberRepo.getFlowRepository().grantFlow(
                        new UserActionDetails(player.getMemberId(), UserAction.COMPLETED_QUEST,
                                              -1, Game.GAME, _gameId, questId), payout);
                }

                // note that we played one game and awarded the specified flow
                _gameRepo.noteGamePlayed(_gameId, 1, payout);

                // mark the quest completed and create a log record
                _repo.noteQuestCompleted(
                    _gameId, player.getMemberId(), questId, playerMins, payoutLevel);

                // if this award consumes the remainder of our awardable flow, recalc our bits
                if (newFlowToNextRecalc > 0) {
                    QuestLogSummaryRecord record = _repo.summarizeQuestLogRecords(_gameId);
                    if (record.payoutFactorTotal > 0) {
                        float targetFlow = flowPerHour * record.playerMinsTotal / 60f;
                        _newFactor = Math.round(targetFlow / record.payoutFactorTotal);

                        _gameRepo.updatePayoutFactor(_gameId, _newFactor, newFlowToNextRecalc);
                        _repo.deleteQuestLogRecords(_gameId);

                        log.info("Recalculation complete [factor=(" + flowPerHour + "*" +
                                 record.playerMinsTotal + ")/60f/" +
                                 record.payoutFactorTotal + " => " + _newFactor + "]");
                    }
                }
            }

            public void handleSuccess () {
                if (oldState != null) {
                    player.removeFromQuestState(questId);
                }

                // if we paid out flow, let any logged-on member objects know
                if (payout > 0) {
                    _worldClient.reportFlowAward(player.getMemberId(), payout);
                    // report to the game that this player earned some flow
                    player.postMessage(AVRGameObject.COINS_AWARDED_MESSAGE, payout, -1);
                }

                // if we updated the payout factor in the db, do it in dobj land too
                if (newFlowToNextRecalc > 0) {
                    _content.detail.payoutFactor = _newFactor;
                }

                reportRequestProcessed();
            }

            protected int _newFactor;
        });
    }

    // from AVRGameProvider
    public void cancelQuest (ClientObject caller, final String questId,
                             InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject) caller;

        QuestState oldState = player.questState.get(questId);
        if (oldState == null) {
            log.warning("Member not subscribed to cancelled quest [game=" + where() +
                        ", quest=" + questId + ", who=" + player.who() + "].");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        _invoker.postUnit(new PersistingUnit("cancelQuest", listener) {
            public void invokePersistent () throws Exception {
                if (!MemberName.isGuest(player.getMemberId())) {
                    _repo.deleteQuestState(player.getMemberId(), _gameId, questId);
                }
            }
            public void handleSuccess () {
                player.removeFromQuestState(questId);
                reportRequestProcessed();
            }
        });
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
        PlayerObject player = getPlayer(playerId);
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
     * Start managing a player who just joined this AVRG, given existing quest and state records.
     */
    public void addPlayer (PlayerObject player, List<QuestStateRecord> questRecords,
                           List<PlayerGameStateRecord> stateRecords)
    {
        if (_players.containsKey(player.getOid())) {
            log.warning("Attempting to re-add existing player [gameId=" + _gameId + ", playerId=" +
                player.getMemberId() + "]");
            return;
        }
        _players.put(player.getOid(), new Player(player));

        _gameObj.startTransaction();
        try {
            _gameObj.addToPlayers(new OccupantInfo(player));
            _gameObj.addToPlayerOids(player.getOid());
        } finally {
            _gameObj.commitTransaction();
        }

        player.startTransaction();
        try {
            for (PlayerGameStateRecord rec : stateRecords) {
                player.addToGameState(rec.toEntry());
            }
            for (QuestStateRecord rec: questRecords) {
                player.addToQuestState(rec.toEntry());
            }
        } finally {
            player.commitTransaction();
        }

        _watchmgr.addWatch(player.getMemberId(), _observer);
        // TODO: make sure the AVRG does not initialize for this player until Room Subscription

        _worldClient.updatePlayer(player.getMemberId(), _content.game);
    }

    /**
     * The given player has quit the AVRG; remove them from the OidList data structure and then
     * do the actual removal in the {@link #objectRemoved(ObjectRemovedEvent)} handler.
     */
    public void removePlayer (PlayerObject player)
    {
        if (!_gameObj.playerOids.contains(player.getOid())) {
            log.warning("Trying to remove unknown player [gameId=" + _gameId + ", playerId=" +
                player.getMemberId() + "]");
            return;
        }
        _gameObj.removeFromPlayerOids(player.getOid());
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
     * Return the total number of seconds that players were being tracked.
     */
    protected int getTotalTrackedSeconds ()
    {
        int total = _totalTrackedSeconds;
        int now = now();

        for (Player player : _players.values()) {
            total += player.getPlayTime(now);
        }
        return total;
    }

    protected PlayerObject getPlayer (int playerId)
    {
        // TODO: we should probably map playerId -> playerOid in this manager object
        for (OccupantInfo occInfo : _gameObj.players) {
            if ((occInfo.username instanceof MemberName) &&
                    playerId == ((MemberName) occInfo.username).getMemberId()) {
                Player player = _players.get(occInfo.getBodyOid());
                return player != null ? player.playerObject : null;
            }
        }
        return null;
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
     * Convenience method to calculate the current timestmap in seconds.
     */
    protected static int now ()
    {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * Reports the id/name of this game for use in log messages.
     */
    protected String where ()
    {
        return String.valueOf(_gameId);
    }

    protected void checkForShutdown ()
    {
        int agents = _gameAgentObj != null ? 1 : 0;
        if (_gameObj.getSubscriberCount() <= agents) {
            log.info("Shutting down avrg", "subs", _gameObj.getSubscriberCount());
            shutdown();
        }
    }   
        
    protected AVRGameAgentObject createGameAgentObject (Game game, MsoyGameDefinition def)
    {   
        String code = def.getServerMediaPath(game.gameId);
        if (code == null) {
            log.warning("No server code for avrg", "game", game);
            return null;
        }
            
        AVRGameAgentObject agent = new AVRGameAgentObject();
        agent.bureauId = BureauTypes.GAME_BUREAU_ID_PREFIX + game.gameId;
        agent.bureauType = BureauTypes.THANE_BUREAU_TYPE;
        agent.code = code;
        if (StringUtil.isBlank(def.server)) {
            agent.className = WhirledGameManager.DEFAULT_SERVER_CLASS;
        } else {
            agent.className = def.server;
        }
        return agent;
    }

    protected class Player
    {
        public PlayerObject playerObject;
        public int beganStamp;
        public int secondsPlayed;

        public Player (PlayerObject playerObject)
        {
            this.playerObject = playerObject;
            this.beganStamp = now();
        }

        public int getPlayTime (int now) {
            int secondsOfPlay = secondsPlayed;
            if (beganStamp != 0) {
                secondsOfPlay += (now - beganStamp);
            }
            return secondsOfPlay;
        }

        public void stopTracking (int endStamp) {
            if (beganStamp != 0) {
                secondsPlayed += endStamp - beganStamp;
                beganStamp = 0;
            }
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

    protected Observer _observer = new Observer() {
        public void memberMoved (int memberId, int sceneId, String hostname, int port) {
            playerEnteredScene(memberId, sceneId, hostname, port);
        }
    };

    /** Timed task to check if we are ready to shut down. */
    protected Interval _shutdownCheck;

    /** Set once we've shutdown to prevent multiple shudowns. */
    protected boolean _shutdown;
    
    /** The gameId of this particular AVRG. */
    protected int _gameId;

    /** The distributed object that both clients and the agent sees. */
    protected AVRGameObject _gameObj;

    /** The distributed object that only our agent sees. */
    AVRGameAgentObject _gameAgentObj;

    /** The metadata for the game being played. */
    protected GameContent _content;

    /** A map to track current AVRG player data, per PlayerObject Oid. */
    protected IntMap<Player> _players = new HashIntMap<Player>();

    /** Tracks player that've moved to a scene not yet subscribed to by the agent. */
    protected IntIntMap _pendingMoves = new IntIntMap();

    /** Counts the total number of seconds that have elapsed during 'tracked' time, for each
     * tracked member that is no longer present with a Player object. */
    protected int _totalTrackedSeconds = 0;

    /** The map of tickers, lazy-initialized. */
    protected HashMap<String, Ticker> _tickers;
    
    /** Observer of our shutdown. */
    protected ShutdownObserver _shutdownObserver;

    // our dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected InvocationManager _invmgr;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected WorldServerClient _worldClient;
    @Inject protected GameWatcherManager _watchmgr;
    @Inject protected AVRGameRepository _repo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected BureauRegistry _breg;
    @Inject protected RootDObjectManager _omgr;

    /** The minimum delay a ticker can have. */
    protected static final int MIN_TICKER_DELAY = 50;

    /** The maximum number of tickers allowed at one time. */
    protected static final int MAX_TICKERS = 3;

    /** idle time before shutting down the manager. */
    protected static final long IDLE_UNLOAD_PERIOD = 60 * 1000L; // in ms
}
