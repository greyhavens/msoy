//
// $Id$

package com.threerings.msoy.avrg.server.persist;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Maintains state for AVR games.
 */
@Singleton @BlockingThread
public class AVRGameRepository extends DepotRepository
{
    /**
     * Computed record for checking if a player has a property without actually loading any. It
     * would be protected but depot needs to create it by reflection.
     */
    @Entity @Computed
    public static class HasPropertyRecord extends PersistentRecord
    {
        @Computed(shadowOf=PlayerGameStateRecord.class)
        public int memberId;
    }

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
                  new Where(PlayerGameStateRecord.MEMBER_ID.in(memberIds)));
    }

    /**
     * Gets the subset of members who have at least one property for the given game id.
     */
    public Set<Integer> getPropertiedMembers (int gameId, Set<Integer> memberIds)
    {
        Set<Integer> propIds = Sets.newHashSet();
        if (!memberIds.isEmpty()) {
            List<QueryClause> clauses = Lists.newArrayList(
                new Where(Ops.and(PlayerGameStateRecord.GAME_ID.eq(gameId),
                                  PlayerGameStateRecord.MEMBER_ID.in(memberIds))),
                new GroupBy(PlayerGameStateRecord.MEMBER_ID),
                new FromOverride(PlayerGameStateRecord.class));
            for (HasPropertyRecord prop : findAll(HasPropertyRecord.class, clauses)) {
                propIds.add(prop.memberId);
            }
        }
        return propIds;
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GameStateRecord.class);
        classes.add(AgentStateRecord.class);
        classes.add(PlayerGameStateRecord.class);
    }
}
