//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals.Equals;
import com.samskivert.depot.operator.Conditionals.In;
import com.samskivert.depot.operator.Logic.And;
import com.samskivert.depot.operator.Logic.Or;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Manages "smart" digital item memory.
 */
@Singleton @BlockingThread
public class MemoryRepository extends DepotRepository
{
    @Inject public MemoryRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads the memory for the specified item.
     */
    public List<MemoryRecord> loadMemory (byte itemType, int itemId)
    {
        return findAll(MemoryRecord.class,
                       new Where(MemoryRecord.ITEM_TYPE, itemType,
                                 MemoryRecord.ITEM_ID, itemId));
    }

    /**
     * Loads up the all memory records for all items with the specified type and ids.
     */
    public List<MemoryRecord> loadMemories (byte itemType, Collection<Integer> itemIds)
    {
        return findAll(
            MemoryRecord.class,
            new Where(new And(new Equals(MemoryRecord.ITEM_TYPE, itemType),
                              new In(MemoryRecord.ITEM_ID, itemIds))));
    }

    /**
     * Loads up the all memory records for all items with the specified type and ids.
     */
    public List<MemoryRecord> loadMemories (Collection<ItemIdent> idents)
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
    {
        // delete, update, or insert...
        if (record.datumValue == null) {
            delete(record);

        } else if (update(record) == 0) {
            insert(record);
        }
    }

    /**
     * Deletes all memories for the specified item.
     */
    public void deleteMemories (final byte itemType, final int itemId)
    {
        deleteAll(MemoryRecord.class,
                  new Where(MemoryRecord.ITEM_TYPE, itemType, MemoryRecord.ITEM_ID, itemId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemoryRecord.class);
    }
}
