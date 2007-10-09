//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Where;

/**
 * Manages the trophy persistent storage.
 */
public class TrophyRepository extends DepotRepository
{
    public TrophyRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    /**
     * Loads all of the specified member's trophies.
     */
    public List<TrophyRecord> loadTrophies (int memberId)
        throws PersistenceException
    {
        return findAll(TrophyRecord.class, new Where(TrophyRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Loads all of the specified member's trophies from the specified game.
     */
    public List<TrophyRecord> loadTrophies (int gameId, int memberId)
        throws PersistenceException
    {
        return findAll(TrophyRecord.class, new Where(TrophyRecord.GAME_ID_C, gameId,
                                                     TrophyRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Returns a list of the idents of the trophies owned by the specified player for the specified
     * game.
     */
    public List<String> loadTrophyOwnership (int gameId, int memberId)
        throws PersistenceException
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
        throws PersistenceException
    {
        insert(trophy);
    }

    /**
     * Deletes all trophies held by the supplied member.
     */
    public void deleteTrophies (final int memberId)
        throws PersistenceException
    {
        deleteAll(TrophyRecord.class, new Where(TrophyRecord.MEMBER_ID_C, memberId),
                  new CacheInvalidator.TraverseWithFilter<TrophyRecord>(TrophyRecord.class) {
                      public boolean testForEviction (Serializable key, TrophyRecord record) {
                          return (record != null) && record.memberId == memberId;
                      }
                  });
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(TrophyRecord.class);
    }
}
