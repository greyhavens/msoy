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
import com.samskivert.jdbc.depot.operator.Logic.And;
import com.samskivert.util.IntSet;

import com.threerings.msoy.world.data.EntityIdent;

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
    public Collection<MemoryRecord> loadMemory (EntityIdent ident)
        throws PersistenceException
    {
        return findAll(MemoryRecord.class, MemoryRecord.makeKey(ident));
    }

    /**
     * Loads up the all memory records for all entities of the specified type.
     */
    public Collection<MemoryRecord> loadMemories (byte type, IntSet entityIds)
        throws PersistenceException
    {
        Integer[] idvec = entityIds.toArray(new Integer[entityIds.size()]);
        return findAll(MemoryRecord.class,
                       new Where(new And(new Equals(MemoryRecord.TYPE, type),
                                         new In(MemoryRecord.ENTITY_ID, idvec))));
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
}
