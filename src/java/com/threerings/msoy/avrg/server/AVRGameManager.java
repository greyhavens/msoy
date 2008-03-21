//
// $Id$

package com.threerings.msoy.avrg.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Interval;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.whirled.data.ScenePlace;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.QuestState;
import com.threerings.msoy.game.server.GameContent;
import com.threerings.msoy.game.server.MsoyGameServer;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.avrg.data.AVRGameMarshaller;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.server.AVRGameDispatcher;
import com.threerings.msoy.avrg.server.persist.AVRGameRepository;
import com.threerings.msoy.avrg.server.persist.GameStateRecord;
import com.threerings.msoy.avrg.server.persist.PlayerGameStateRecord;
import com.threerings.msoy.avrg.server.persist.QuestLogSummaryRecord;
import com.threerings.msoy.avrg.server.persist.QuestStateRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages an AVR game on the server.
 */
public class AVRGameManager
    implements AVRGameProvider, OidListListener
{
    public AVRGameManager (int gameId, AVRGameRepository repo)
    {
        _gameId = gameId;
        _repo = repo;
    }

    public AVRGameObject createGameObject ()
    {
        return new AVRGameObject();
    }

    public AVRGameObject getGameObject ()
    {
        return _gameObj;
    }

    public int getGameId ()
    {
        return _gameId;
    }

    public void startup (AVRGameObject gameObj, GameContent content,
                         List<GameStateRecord> stateRecords)
    {
        _gameObj = gameObj;
        _content = content;

        // listen for gameObj.playerOids removals
        gameObj.addListener(this);

        gameObj.setAvrgService((AVRGameMarshaller) MsoyGameServer.invmgr.registerDispatcher(
            new AVRGameDispatcher(this)));

        gameObj.startTransaction();
        gameObj.setGameMedia(_content.game.gameMedia);
        try {
            for (GameStateRecord rec : stateRecords) {
                gameObj.addToState(rec.toEntry());
            }
        } finally {
            gameObj.commitTransaction();
        }
    }

    /** The game has changed while we're hosting it; update the media, the client will reload. */
    public void updateGame (Game game)
    {
        _gameObj.setGameMedia(game.gameMedia);
    }

    public void shutdown ()
    {
        stopTickers();
        
        // identify any modified memory records for flushing to the database
        final List<GameStateRecord> recs = new ArrayList<GameStateRecord>();
        for (GameState entry : _gameObj.state) {
            if (entry.persistent && entry.modified) {
                recs.add(new GameStateRecord(_gameId, entry));
            }
        }
        
        final int totalMinutes = Math.max(1, Math.round(getTotalTrackedSeconds() / 60f));

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("shutdown") {
            public void invokePersist () throws Exception {
                for (GameStateRecord rec : recs) {
                    _repo.storeState(rec);
                }
                MsoyGameServer.gameReg.getGameRepository().noteGamePlayed(_gameId, 0, totalMinutes);
            }
            public void handleSuccess () {
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to flush game state [gameId=" + _gameId + "]", pe);
            }
        });
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
                log.warning(
                    "Player removed from OidList without a corresponding DSet entry [gameId=" +
                    _gameId + ", oid=" + event.getOid() + "]");
            }
            Player player = _players.remove(playerOid);
            if (player == null) {
                log.warning(
                    "Eek, unregistered player vanished from OidList [gameId=" + _gameId +
                    "playerOid=" + playerOid + "]");
                return;
            }

            _totalTrackedSeconds += player.getPlayTime(now());
            
            flushPlayerGameState(player.playerObject);

            MsoyGameServer.worldClient.updatePlayer(player.playerObject.getMemberId(), null);
        }
    }

    // from AVRGameProvider
    public void startQuest (ClientObject caller, final String questId, final String status,
                            final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject) caller;

        if (player.questState.containsKey(questId)) {
            // silently ignore
            return;
        }

        final int sceneId = ScenePlace.getSceneId(player);

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("startQuest") {
            public void invokePersist () throws PersistenceException {
                _repo.setQuestState(
                    _gameId, player.getMemberId(), questId, QuestState.STEP_FIRST, status, sceneId);
            }
            public void handleSuccess () {
                player.addToQuestState(
                    new QuestState(questId, QuestState.STEP_FIRST, status, sceneId));
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.log(
                    Level.WARNING, "Unable to subscribe to quest [questId=" + questId + "]", pe);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // from AVRGameProvider
    public void updateQuest (ClientObject caller, final String questId, final int step,
                             final String status, final ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject) caller;

        QuestState oldState = player.questState.get(questId);
        if (oldState == null) {
            throw new IllegalArgumentException(
                "Member not subscribed to updated quest [questId=" + questId + "]");
        }

        final int sceneId = ScenePlace.getSceneId(player);

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("updateQuest") {
            public void invokePersist () throws PersistenceException {
                _repo.setQuestState(_gameId, player.getMemberId(), questId, step, status, sceneId);
            }
            public void handleSuccess () {
                player.updateQuestState(new QuestState(questId, step, status, sceneId));
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to advance quest [questId=" + questId + ", step=" +
                    step + "]", pe);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // from AVRGameProvider
    public void completeQuest (ClientObject caller, final String questId, final float payoutLevel,
                               final ConfirmListener listener)
        throws InvocationException
    {
        if (payoutLevel < 0 || payoutLevel > 1) {
            throw new IllegalArgumentException(
                "Payout level must lie in [0, 1] [payoutLevel=" + payoutLevel + "]");
        }
        final PlayerObject player = (PlayerObject) caller;

        final QuestState oldState = player.questState.get(questId);
//        if (oldState == null) {
//            throw new IllegalArgumentException(
//                "Member not subscribed to completed quest [questId=" + questId + "]");
//        }

        final int flowPerHour = RuntimeConfig.server.hourlyGameFlowRate;
        final int recalcMinutes;
        final int oldPayoutFactor;
        
        // the tutorial gets to pay out fixed amounts of flow; all other games are dynamic
        if (_gameId == Game.TUTORIAL_GAME_ID) {
            oldPayoutFactor = 1;
            recalcMinutes = 0;
            
        } else {
            if (_content.detail.payoutFactor == 0) {
                // if we've yet to accumulate enough data for a calculation, guesstimate 5 mins
                oldPayoutFactor = Math.round(flowPerHour * (5 / 3600f));
            } else {
                oldPayoutFactor = _content.detail.payoutFactor;
            }
            log.info("singlePlayerGames: " + _content.detail.singlePlayerGames);
            // TODO: Change the "100" into a runtime configuration value
            if (((_content.detail.singlePlayerGames + 1) % 100) == 0) {
                recalcMinutes = Math.round(getTotalTrackedSeconds() / 60f) +
                    (_content.detail.singlePlayerMinutes -  _content.detail.lastPayoutRecalc);
                log.info("Recalculation mins: " +  Math.round(getTotalTrackedSeconds()) +
                    " + (" + _content.detail.singlePlayerMinutes + " - " +
                    _content.detail.lastPayoutRecalc + ") = " + recalcMinutes);
            } else {
                recalcMinutes = 0;
            }
        }
        
        MsoyGameServer.invoker.postUnit(new RepositoryUnit("completeQuest") {
            public void invokePersist () throws PersistenceException {
                GameRepository gameRepo = MsoyGameServer.gameReg.getGameRepository();

                _payoutFactor = oldPayoutFactor;
                // see if it's time to recalculate the payout factor
                if (recalcMinutes > 0) {
                    QuestLogSummaryRecord record = _repo.summarizeQuestLogRecords(_gameId);
                    if (record.payoutFactorTotal > 0) {
                        _payoutFactor = (flowPerHour * recalcMinutes) / 60 / record.payoutFactorTotal;
                        log.info("new payout factor = (" + flowPerHour + " *" + recalcMinutes +
                            ") / 60 / " + record.payoutFactorTotal + " = " + _payoutFactor);
                    }

                    gameRepo.updatePayoutFactor(_gameId, _payoutFactor);
                }

                // mark the quest completed and create a log record
                _repo.noteQuestCompleted(_gameId, player.getMemberId(), questId, payoutLevel);

                // bump the "games played" count by one
                MsoyGameServer.gameReg.getGameRepository().noteGamePlayed(_gameId, 1, 0);

                // now award the flow
                _payout = Math.round(_payoutFactor * payoutLevel); 
                if (_payout > 0) {
                    MsoyGameServer.memberRepo.getFlowRepository().grantFlow(
                        new UserActionDetails(player.getMemberId(), UserAction.COMPLETED_QUEST,
                                -1, Game.GAME, _gameId, questId), 
                        _payout);
                }
            }
            public void handleSuccess () {
                if (oldState != null) {
                    player.removeFromQuestState(questId);
                }

                log.info("Paying out flow: " + _payout);
                // if we paid out flow, let any logged-on member objects know
                if (_payout > 0) {
                    MsoyGameServer.worldClient.reportFlowAward(player.getMemberId(), _payout);

                    // report to the game that this player earned some flow
                    DObject user = MsoyGameServer.omgr.getObject(player.getOid());
                    if (user != null) {
                        user.postMessage(AVRGameObject.COINS_AWARDED_MESSAGE, _payout, -1);
                    }
                }

                // note the completion in our runtime data structure
                _content.detail.singlePlayerGames ++;
                
                // if we updated the payout factor in the db, do it in dobj land too
                if (_payoutFactor != oldPayoutFactor) {
                    _content.detail.payoutFactor = _payoutFactor;
                }
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to complete quest [questId=" + questId + "]", pe);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
            
            protected int _payout;
            protected int _payoutFactor;
        });
    }

    protected int getPlayerMinutes ()
    {
        // TODO: iterate over tracking records
        return 0;
    }

    // from AVRGameProvider
    public void cancelQuest (ClientObject caller, final String questId,
                             final ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject) caller;

        QuestState oldState = player.questState.get(questId);
        if (oldState == null) {
            throw new IllegalArgumentException(
                "Member not subscribed to cancelled quest [questId=" + questId + "]");
        }
        MsoyGameServer.invoker.postUnit(new RepositoryUnit("cancelQuest") {
            public void invokePersist () throws PersistenceException {
                _repo.deleteQuestState(player.getMemberId(), _gameId, questId);
            }
            public void handleSuccess () {
                player.removeFromQuestState(questId);
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to cancel quest [questId=" + questId + "]", pe);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // from AVRGameProvider
    public void setProperty (ClientObject caller, String key, byte[] value, boolean persistent,
                             ConfirmListener listener)
        throws InvocationException
    {
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
    public void deleteProperty (ClientObject caller, String key, ConfirmListener listener)
        throws InvocationException
    {
        setProperty(caller, key, null, true, listener);
    }

    // from AVRGameProvider
    public void setPlayerProperty (ClientObject caller, String key, byte[] value,
                                   boolean persistent, ConfirmListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject) caller;

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
    public void deletePlayerProperty (ClientObject caller, String key, ConfirmListener listener)
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
        if (_gameObj.playerOids.contains(playerId)) {
            PlayerObject toPlayer = (PlayerObject) MsoyGameServer.omgr.getObject(playerId);
            if (toPlayer != null) {
                toPlayer.postMessage(
                    AVRGameObject.USER_MESSAGE + ":" + _gameObj.getOid(),
                    new Object[] { msg, data });
            }
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

    public void addPlayer (PlayerObject player, List<QuestStateRecord> questRecords,
                           List<PlayerGameStateRecord> stateRecords)
    {
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

        MsoyGameServer.worldClient.updatePlayer(player.getMemberId(), _content.game);
    }

    public void removePlayer (PlayerObject player)
    {
        if (!_gameObj.playerOids.contains(player.getOid())) {
            log.warning("Trying to remove unknown player [gameId=" + _gameId + ", playerId=" +
                player.getMemberId() + "]");
            return;
        }
        _gameObj.removeFromPlayerOids(player.getOid());
    }

    protected void flushPlayerGameState (final PlayerObject player)
    {
        // find any modified memory records
        final List<PlayerGameStateRecord> recs = new ArrayList<PlayerGameStateRecord>();
        for (GameState entry : player.gameState) {
            if (entry.persistent && entry.modified) {
                recs.add(new PlayerGameStateRecord(_gameId, player.getMemberId(), entry));
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
        MsoyGameServer.invoker.postUnit(new RepositoryUnit("removePlayer") {
            public void invokePersist () throws Exception {
                for (PlayerGameStateRecord rec : recs) {
                    _repo.storePlayerState(rec);
                }
            }
            public void handleSuccess () {
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to flush player game state [gameId=" + _gameId +
                    "player=" + player + "]", pe);
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

    protected class Player
    {
        public PlayerObject playerObject;
        public int beganStamp;
        public int secondsPlayed;

        public Player (PlayerObject playerObject)
        {
            this.playerObject = playerObject;
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

    protected int _gameId;

    /** Counts the total number of seconds that have elapsed during 'tracked' time, for each
     * tracked member that is no longer present with a Player object. */
    protected int _totalTrackedSeconds = 0;

    /** The map of tickers, lazy-initialized. */
    protected HashMap<String, Ticker> _tickers;

    protected GameContent _content;
    
    protected AVRGameObject _gameObj;
    
    protected AVRGameRepository _repo;
    
    protected IntMap<Player> _players = new HashIntMap<Player>();
    
    /** The minimum delay a ticker can have. */
    protected static final int MIN_TICKER_DELAY = 50;

    /** The maximum number of tickers allowed at one time. */
    protected static final int MAX_TICKERS = 3;
}
