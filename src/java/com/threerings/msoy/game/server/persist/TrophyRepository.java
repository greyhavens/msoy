//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.util.List;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;

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
        return null; // TODO
    }

    /**
     * Loads all of the specified member's trophies from the specified game.
     */
    public List<TrophyRecord> loadTrophies (int gameId, int memberId)
        throws PersistenceException
    {
        return null; // TODO
    }

    /**
     * Stores the supplied trophy record in the database.
     */
    public void storeTrophy (TrophyRecord trophy)
        throws PersistenceException
    {
        // TODO
    }

    /**
     * Deletes all trophies held by the supplied member.
     */
    public void deleteTrophies (int memberId)
        throws PersistenceException
    {
        // TODO
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(TrophyRecord.class);
    }
}
