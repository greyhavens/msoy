//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.util.Collection;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Conditionals.In;
import com.samskivert.jdbc.depot.operator.Logic.And;

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
     * Loads the memory for the specified item.
     */
    public Collection<MemoryRecord> loadMemory (byte itemType, int itemId)
        throws PersistenceException
    {
        return findAll(MemoryRecord.class, new Key<MemoryRecord>(
                           MemoryRecord.class, MemoryRecord.ITEM_TYPE, itemType,
                           MemoryRecord.ITEM_ID, itemId));
    }

    /**
     * Loads up the all memory records for all items with the specified type and ids.
     */
    public Collection<MemoryRecord> loadMemories (byte itemType, Collection<Integer> itemIds)
        throws PersistenceException
    {
        return findAll(MemoryRecord.class,
                       new Where(new And(new Equals(MemoryRecord.ITEM_TYPE, itemType),
                                         new In(MemoryRecord.ITEM_ID, itemIds))));
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
