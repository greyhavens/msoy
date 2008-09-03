//
// $Id$

package com.threerings.msoy.avrg.server.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FieldDefinition;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.FunctionExp;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.game.data.QuestState;

/**
 * Maintains state for AVR games.
 */
@Singleton @BlockingThread
public class AVRGameRepository extends DepotRepository
{
    @Inject public AVRGameRepository (PersistenceContext context)
    {
        super(context);
    }

    public List<QuestStateRecord> getQuests (int memberId)
    {
        return findAll(QuestStateRecord.class, new Where(QuestStateRecord.MEMBER_ID_C, memberId));
    }

    public List<QuestStateRecord> getQuests (int gameId, int memberId)
    {
        return findAll(QuestStateRecord.class, new Where(new And(
                new Equals(QuestStateRecord.GAME_ID_C, gameId),
                new Equals(QuestStateRecord.MEMBER_ID_C, memberId),
                new Not(new In(QuestStateRecord.STEP_C,
                    QuestState.STEP_COMPLETED, QuestState.STEP_VIRGIN)))));
    }

    public void setQuestState (
        int gameId, int memberId, String questId, int step, String status, int sceneId)
    {
        store(new QuestStateRecord(memberId, gameId, questId, step, status, sceneId));
    }

    public void deleteQuestState (int memberId, int gameId, String questId)
    {
        delete(QuestStateRecord.class, QuestStateRecord.getKey(memberId, gameId, questId));
    }

    public List<PlayerGameStateRecord> getPlayerGameState (int gameId, int memberId)
    {
        return findAll(PlayerGameStateRecord.class, new Where(
            PlayerGameStateRecord.GAME_ID_C, gameId,
            PlayerGameStateRecord.MEMBER_ID_C, memberId));
    }

    public List<GameStateRecord> getGameState (int gameId)
    {
        return findAll(GameStateRecord.class, new Where(GameStateRecord.GAME_ID_C, gameId));
    }

    /**
     * Stores a particular memory record in the repository.
     */
    public void storeState (GameStateRecord record)
    {
        if (record.datumValue == null) {
            delete(record);

        } else {
            store(record);
        }
    }

    /**
     * Stores a particular memory record in the repository.
     */
    public void storePlayerState (PlayerGameStateRecord record)
    {
        if (record.datumValue == null) {
            delete(record);

        } else {
            store(record);
        }
    }

    public void deleteProperty (int gameId, String key)
    {
        delete(GameStateRecord.class, GameStateRecord.getKey(gameId, key));
    }

    public void deletePlayerProperty (int gameId, int memberId, String key)
    {
        delete(PlayerGameStateRecord.class, PlayerGameStateRecord.getKey(gameId, memberId, key));
    }

    public void noteQuestCompleted (int gameId, int memberId, String questId,
                                    int playerMins, float payoutFactor)
    {
        gameId = Math.abs(gameId); // how to handle playing the original?
        setQuestState(gameId, memberId, questId, QuestState.STEP_COMPLETED, null, 0);
        insert(new QuestLogRecord(gameId, memberId, questId, playerMins, payoutFactor));
    }

    public void noteUnawardedTime (int gameId, int playerMins)
    {
        insert(new QuestLogRecord(gameId, 0, "", playerMins, 0));
    }

    public QuestLogSummaryRecord summarizeQuestLogRecords (int gameId)
    {
        gameId = Math.abs(gameId); // how to handle playing the original?
        return load(
            QuestLogSummaryRecord.class,
            new Where(QuestLogRecord.GAME_ID_C, gameId),
            new FromOverride(QuestLogRecord.class),
            new FieldDefinition(QuestLogSummaryRecord.GAME_ID,
                                QuestLogRecord.GAME_ID_C),
            new FieldDefinition(QuestLogSummaryRecord.PLAYER_MINS_TOTAL,
                                new FunctionExp("sum", QuestLogRecord.PLAYER_MINS_C)),
            new FieldDefinition(QuestLogSummaryRecord.PAYOUT_FACTOR_TOTAL,
                                new FunctionExp("sum", QuestLogRecord.PAYOUT_FACTOR_C)),
            new FieldDefinition(QuestLogSummaryRecord.PAYOUT_COUNT,
                                new LiteralExp("count(*)")),
            new GroupBy(QuestLogRecord.GAME_ID_C));
    }

    public void deleteQuestLogRecords (int gameId)
    {
        gameId = Math.abs(gameId); // how to handle playing the original?
        deleteAll(QuestLogRecord.class, new Where(QuestLogRecord.GAME_ID_C, gameId), null);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GameStateRecord.class);
        classes.add(PlayerGameStateRecord.class);
        classes.add(QuestStateRecord.class);
        classes.add(QuestLogRecord.class);
    }
}
