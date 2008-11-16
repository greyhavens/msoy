//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Manages the trophy persistent storage.
 */
@Singleton @BlockingThread
public class TrophyRepository extends DepotRepository
{
    @Inject public TrophyRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    /**
     * Loads all of the specified member's trophies.
     */
    public List<TrophyRecord> loadTrophies (int memberId)
    {
        return findAll(TrophyRecord.class, new Where(TrophyRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Loads the specified number of recently earned trophies for the specified member.
     */
    public List<TrophyRecord> loadRecentTrophies (int memberId, int count)
    {
        return findAll(TrophyRecord.class, new Where(TrophyRecord.MEMBER_ID_C, memberId),
                       OrderBy.descending(TrophyRecord.WHEN_EARNED_C),
                       new Limit(0, count));
    }

    /**
     * Loads all of the specified member's trophies from the specified game.
     */
    public List<TrophyRecord> loadTrophies (int gameId, int memberId)
    {
        return findAll(TrophyRecord.class, new Where(TrophyRecord.GAME_ID_C, gameId,
                                                     TrophyRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Returns a list of the idents of the trophies owned by the specified player for the specified
     * game.
     */
    public List<String> loadTrophyOwnership (int gameId, int memberId)
    {
        ArrayList<String> idents = new ArrayList<String>();
        Where where = new Where(TrophyRecord.GAME_ID_C, gameId, TrophyRecord.MEMBER_ID_C, memberId);
        for (TrophyOwnershipRecord orec : findAll(TrophyOwnershipRecord.class, where)) {
            idents.add(orec.ident);
        }
        return idents;
    }

    /**
     * Stores the supplied trophy record in the database.
     */
    public void storeTrophy (TrophyRecord trophy)
    {
        insert(trophy);
    }

    /**
     * Deletes all trophies held by the supplied member.
     */
    public void deleteTrophies (final int memberId)
    {
        deleteAll(TrophyRecord.class, new Where(TrophyRecord.MEMBER_ID_C, memberId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(TrophyRecord.class);
    }
}
