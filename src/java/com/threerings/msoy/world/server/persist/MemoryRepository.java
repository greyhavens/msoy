//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.util.Collection;
import java.util.List;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Conditionals.In;
import com.samskivert.jdbc.depot.operator.Logic.And;
import com.samskivert.jdbc.depot.operator.Logic.Or;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;

import com.threerings.msoy.item.data.all.ItemIdent;

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
    public List<MemoryRecord> loadMemory (byte itemType, int itemId)
        throws PersistenceException
    {
        return findAll(MemoryRecord.class,
                       new Where(MemoryRecord.ITEM_TYPE, itemType,
                                 MemoryRecord.ITEM_ID, itemId));
    }

    /**
     * Loads up the all memory records for all items with the specified type and ids.
     */
    public List<MemoryRecord> loadMemories (byte itemType, Collection<Integer> itemIds)
        throws PersistenceException
    {
        return findAll(MemoryRecord.class,
                       new Where(new And(new Equals(MemoryRecord.ITEM_TYPE, itemType),
                                         new In(MemoryRecord.ITEM_ID, itemIds))));
    }

    /**
     * Loads up the all memory records for all items with the specified type and ids.
     */
    public List<MemoryRecord> loadMemories (Collection<ItemIdent> idents)
        throws PersistenceException
    {
        HashIntMap<ArrayIntSet> types = new HashIntMap<ArrayIntSet>();
        for (ItemIdent ident : idents) {
            ArrayIntSet typeSet = types.get(ident.type);
            if (typeSet == null) {
                types.put(ident.type, typeSet = new ArrayIntSet());
            }
            typeSet.add(ident.itemId);
        }

        And[] eachType = new And[types.size()];
        int index = 0;
        for (IntMap.IntEntry<ArrayIntSet> entry : types.intEntrySet()) {
            eachType[index++] = new And(new Equals(MemoryRecord.ITEM_TYPE, entry.getIntKey()),
                new In(MemoryRecord.ITEM_ID, entry.getValue()));
        }

        return findAll(MemoryRecord.class, new Where(new Or(eachType)));
    }

    /**
     * Stores all supplied memory records.
     */
    public void storeMemories (Collection<MemoryRecord> records)
        throws PersistenceException
    {
        // TODO: if one storeMemory() fails, should we catch that error, keep going, then
        // consolidate the errors and throw a single error?
        for (MemoryRecord record : records) {
            storeMemory(record);
        }
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
