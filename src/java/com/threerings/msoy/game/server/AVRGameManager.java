//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.threerings.msoy.game.data.AVRGameMarshaller;
import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.QuestState;
import com.threerings.msoy.game.server.persist.AVRGameRepository;
import com.threerings.msoy.game.server.persist.GameStateRecord;
import com.threerings.msoy.game.server.persist.PlayerGameStateRecord;
import com.threerings.msoy.item.data.all.Game;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;
import com.threerings.presents.server.InvocationException;
import com.threerings.whirled.data.ScenePlace;

import static com.threerings.msoy.Log.log;

/**
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

    public void startup (AVRGameObject gameObj, Game game, List<GameStateRecord> stateRecords)
    {
        _gameObj = gameObj;
        _game = game;

        // listen for gameObj.playerOids removals
        gameObj.addListener(this);

        gameObj.setAvrgService((AVRGameMarshaller) MsoyGameServer.invmgr.registerDispatcher(
            new AVRGameDispatcher(this)));

        gameObj.startTransaction();
        gameObj.setGameMedia(game.gameMedia);
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
            if (entry.persistent && entry.modified) {
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
            PlayerObject player = _players.remove(playerOid);
            assert(player != null);
            flushPlayerGameState(player);
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
                    player.getMemberId(), _gameId, questId, QuestState.STEP_NEW, status, sceneId);
            }
            public void handleSuccess () {
                player.addToQuestState(
                    new QuestState(questId, QuestState.STEP_NEW, status, sceneId));
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
                _repo.setQuestState(player.getMemberId(), _gameId, questId, step, status, sceneId);
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

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("completeQuest") {
            public void invokePersist () throws PersistenceException {
                _repo.setQuestState(
                    player.getMemberId(), _gameId, questId, QuestState.STEP_COMPLETED, null, 0);
            }
            public void handleSuccess () {
                player.removeFromQuestState(questId);
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to complete quest [questId=" + questId + "]", pe);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
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

    public void addPlayer (PlayerObject player, List<PlayerGameStateRecord> stateRecords)
    {
        _players.put(player.getOid(), player);

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
        } finally {
            player.commitTransaction();
        }

        MsoyGameServer.worldClient.updatePlayer(player.getMemberId(), _game);
    }

    public void removePlayer (PlayerObject player)
    {
        _gameObj.removeFromPlayerOids(player.getOid());

        MsoyGameServer.worldClient.updatePlayer(player.getMemberId(), null);
    }

    protected void flushPlayerGameState (final PlayerObject player)
    {
        // flush any modified memory records to the database
        final List<PlayerGameStateRecord> recs = new ArrayList<PlayerGameStateRecord>();
        for (GameState entry : player.gameState) {
            if (entry.persistent && entry.modified) {
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
                log.log(Level.WARNING, "Unable to flush player game state [gameId=" + _gameId +
                    "player=" + player + "]", pe);
            }
        });
    }

    protected int _gameId;

    protected Game _game;

    protected AVRGameObject _gameObj;

    protected AVRGameRepository _repo;

    protected IntMap<PlayerObject> _players = new HashIntMap<PlayerObject>();
}
