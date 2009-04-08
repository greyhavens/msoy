//
// $Id$

package com.threerings.msoy.avrg.server.persist;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

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

    public List<AgentStateRecord> getAgentState (int gameId)
    {
        return findAll(AgentStateRecord.class, new Where(AgentStateRecord.GAME_ID, gameId));
    }

    public List<GameStateRecord> getGameState (int gameId)
    {
        return findAll(GameStateRecord.class, new Where(GameStateRecord.GAME_ID, gameId));
    }

    public List<PlayerGameStateRecord> getPlayerGameState (int gameId, int memberId)
    {
        return findAll(PlayerGameStateRecord.class, new Where(
            PlayerGameStateRecord.GAME_ID, gameId,
            PlayerGameStateRecord.MEMBER_ID, memberId));
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
    public void storeAgentState (AgentStateRecord record)
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

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        deleteAll(PlayerGameStateRecord.class,
                  new Where(new Conditionals.In(PlayerGameStateRecord.MEMBER_ID, memberIds)));
    }

    /**
     * Gets the subset of members who have at least one property for the given game id.
     */
    public Set<Integer> getPropertiedMembers (int gameId, Set<Integer> memberIds)
    {
        // TODO
        // select "memberId", count(*) from "PlayerGameStateRecord" where "gameId" = {gameId} and
        //     "memberId" in {memberIds} group by "memberId";
        return Collections.emptySet();
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GameStateRecord.class);
        classes.add(AgentStateRecord.class);
        classes.add(PlayerGameStateRecord.class);
    }
}
