//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.util.Collection;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Conditionals.In;

/**
 * Manages "smart" digital item memory.
 */
public class MemoryRepository extends DepotRepository
{
    public MemoryRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    /**
     * Loads the memory for the specified entity.
     */
    public Collection<MemoryRecord> loadMemory (int memoryId)
        throws PersistenceException
    {
        return findAll(MemoryRecord.class, new Where(new Equals(MemoryRecord.MEMORY_ID, memoryId)));
    }

    /**
     * Loads up the all memory records for all entities of the specified type.
     */
    public Collection<MemoryRecord> loadMemories (Collection<Integer> memoryIds)
        throws PersistenceException
    {
        return findAll(MemoryRecord.class, new Where(new In(MemoryRecord.MEMORY_ID, memoryIds)));
    }

    /**
     * Stores a particular memory record in the repository.
     */
    public void storeMemory (MemoryRecord record)
        throws PersistenceException
    {
        // we assume that most of the time we'll be updating, so optimize for that
        if (update(record) == 0) {
            insert(record);
        }
    }

    /**
     * Updates all supplied memory records. The records are all assumed to already exist in the
     * database.
     */
    public void updateMemories (Collection<MemoryRecord> records)
        throws PersistenceException
    {
        // TODO: if one update() fails, should we catch that error, keep going, then consolidate
        // the errors and throw a single error?
        for (MemoryRecord record : records) {
            update(record);
        }
    }
}
