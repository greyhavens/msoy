//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.util.List;
import java.util.Set;


import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Where;

public class AVRGameRepository extends DepotRepository
{
    public AVRGameRepository (PersistenceContext context)
    {
        super(context);
    }

    public List<QuestStateRecord> loadQuestsForMember (int memberId)
        throws PersistenceException
    {
        return findAll(QuestStateRecord.class, new Where(QuestStateRecord.MEMBER_ID_C, memberId));
    }

    public void setQuestState (int gameId, String questId, int step, String status, int sceneId)
        throws PersistenceException
    {
        store(new QuestStateRecord(gameId, questId, step, status, sceneId));
    }

    public void deleteQuestState (int memberId, int gameId, String questId)
        throws PersistenceException
    {
        delete(QuestStateRecord.class, QuestStateRecord.getKey(memberId, gameId, questId));
    }

    public List<PlayerGameStateRecord> getPlayerGameState (int gameId, int memberId)
        throws PersistenceException
    {
        return findAll(PlayerGameStateRecord.class, new Where(
            PlayerGameStateRecord.GAME_ID_C, gameId,
            PlayerGameStateRecord.MEMBER_ID_C, memberId));
    }

    public List<GameStateRecord> getGameState (int gameId)
        throws PersistenceException
    {
        return findAll(GameStateRecord.class, new Where(GameStateRecord.GAME_ID_C, gameId));
    }

    /**
     * Stores a particular memory record in the repository.
     */
    public void storeState (GameStateRecord record)
        throws PersistenceException
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
        throws PersistenceException
    {
        if (record.datumValue == null) {
            delete(record);

        } else {
            store(record);
        }
    }

    public void deleteProperty (int gameId, String key)
        throws PersistenceException
    {
        delete(GameStateRecord.class, GameStateRecord.getKey(gameId, key));
    }

    public void deletePlayerProperty (int gameId, int memberId, String key)
        throws PersistenceException
    {
        delete(PlayerGameStateRecord.class, PlayerGameStateRecord.getKey(gameId, memberId, key));
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GameStateRecord.class);
        classes.add(PlayerGameStateRecord.class);
        classes.add(QuestStateRecord.class);
    }
}
