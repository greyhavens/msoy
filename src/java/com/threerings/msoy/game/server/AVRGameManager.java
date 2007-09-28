//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.List;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.QuestState;
import com.threerings.msoy.game.server.persist.AVRGameRepository;
import com.threerings.msoy.game.server.persist.GameStateRecord;
import com.threerings.msoy.game.server.persist.PlayerGameStateRecord;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.whirled.data.ScenePlace;

import static com.threerings.msoy.Log.log;

/**
 */
public class AVRGameManager
    implements AVRGameProvider
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

    public void startup (AVRGameObject gameObj, List<GameStateRecord> stateRecords)
    {
        _gameObj = gameObj;

        gameObj.startTransaction();
        try {
            for (GameStateRecord rec : stateRecords) {
                gameObj.addToState(rec.toEntry());
            }
        } finally {
            gameObj.commitTransaction();
        }
}

    public void shutdown ()
    {
        // flush any modified memory records to the database
        final List<GameStateRecord> recs = new ArrayList<GameStateRecord>();
        for (GameState entry : _gameObj.state) {
            if (entry.modified) {
                recs.add(new GameStateRecord(_gameId, entry));
            }
        }
        if (recs.size() == 0) {
            return;
        }
        MsoyGameServer.invoker.postUnit(new RepositoryUnit("shutdown") {
            public void invokePersist () throws Exception {
                for (GameStateRecord rec : recs) {
                    _repo.storeState(rec);
                }
            }
            public void handleSuccess () {
            }
            public void handleFailure (Exception pe) {
                log.warning(
                    "Unable to flush game state [gameId=" + _gameId + ", error=" + pe + "]");
            }
        });
    }

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
                _repo.setQuestState(_gameId, questId, QuestState.STEP_NEW, status, sceneId);
            }
            public void handleSuccess () {
                player.addToQuestState(new QuestState(questId, QuestState.STEP_NEW, status, sceneId));
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.warning(
                    "Unable to subscribe to quest [questId=" + questId + ", error=" + pe + "]");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }


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
                _repo.setQuestState(_gameId, questId, step, status, sceneId);
            }
            public void handleSuccess () {
                player.updateQuestState(new QuestState(questId, step, status, sceneId));
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.warning(
                    "Unable to advance quest [questId=" + questId + ", step=" + step +
                    ", error=" + pe + "]");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    public void completeQuest (ClientObject caller, final String questId, int payoutLevel,
                               final ConfirmListener listener)
        throws InvocationException
    {
        // TODO: Handle Flow Payout

        final PlayerObject player = (PlayerObject) caller;

        QuestState oldState = player.questState.get(questId);
        if (oldState == null) {
            throw new IllegalArgumentException(
                "Member not subscribed to updated quest [questId=" + questId + "]");
        }

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("updateQuest") {
            public void invokePersist () throws PersistenceException {
                _repo.setQuestState(_gameId, questId, QuestState.STEP_COMPLETED, null, 0);
            }
            public void handleSuccess () {
                player.removeFromQuestState(questId);
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.warning(
                    "Unable to complete quest [questId=" + questId + ", error=" + pe + "]");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // from AVRGameProvider
    public void setProperty (ClientObject caller, String key, byte[] value,
                             ConfirmListener listener)
        throws InvocationException
    {
        GameState entry = new GameState(key, value);

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
        setProperty(caller, key, null, listener);
    }

    // from AVRGameProvider
    public void setPlayerProperty (ClientObject caller, String key, byte[] value,
                                   ConfirmListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject) caller;

        GameState entry = new GameState(key, value);

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
        setPlayerProperty(caller, key, null, listener);
    }

    public void addPlayer (final PlayerObject player, List<PlayerGameStateRecord> stateRecords)
    {
        // TODO: create & add OccupantInfo, sanity checks

        player.startTransaction();
        try {
            for (PlayerGameStateRecord rec : stateRecords) {
                player.addToGameState(rec.toEntry());
            }
        } finally {
            player.commitTransaction();
        }
    }

    public void removePlayer (final PlayerObject player)
    {
        // TODO: remove OccupantInfo, sanity checks

        // flush any modified memory records to the database
        final List<PlayerGameStateRecord> recs = new ArrayList<PlayerGameStateRecord>();
        for (GameState entry : player.gameState) {
            if (entry.modified) {
                recs.add(new PlayerGameStateRecord(_gameId, player.getMemberId(), entry));
            }
        }
        if (recs.size() == 0) {
            return;
        }
        MsoyGameServer.invoker.postUnit(new RepositoryUnit("removePlayer") {
            public void invokePersist () throws Exception {
                for (PlayerGameStateRecord rec : recs) {
                    _repo.storePlayerState(rec);
                }
            }
            public void handleSuccess () {
            }
            public void handleFailure (Exception pe) {
                log.warning(
                    "Unable to flush player game state [gameId=" + _gameId +  "player=" + player +
                    ", error=" + pe + "]");
            }
        });
    }

    protected int _gameId;

    protected AVRGameObject _gameObj;

    protected AVRGameRepository _repo;
}
