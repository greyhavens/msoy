//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
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

import static com.threerings.msoy.Log.log;

/**
 * Manages "smart" digital item memory.
 */
@Singleton @BlockingThread
public class MemoryRepository extends DepotRepository
{
    @Inject public MemoryRepository (PersistenceContext ctx)
    {
        super(ctx);

        registerMigration(new DataMigration("2009_01_26_convertMemories") {
            @Override public void invoke () throws DatabaseException {
                log.info("Memory migrate: starting");
                List<Key<MemoryRecord>> oldKeys = findAllKeys(MemoryRecord.class, true);
                log.info("Memory migrate: found keys", "memories", oldKeys.size());
                // grind through these keys and build a set of itemIds
                Set<ItemIdent> ids = Sets.newHashSet();
                for (Key<MemoryRecord> key : oldKeys) {
                    ids.add(new ItemIdent(
                        ((Number)key.getValues()[0]).byteValue(),
                        ((Number)key.getValues()[1]).intValue()));
                }
                oldKeys = null; // allow for GC
                // now load all memories for each entity and turn them into a MemoriesRecord
                log.info("Memory migrate: identified entities", "entities", ids.size());
                int count = 0;
                for (ItemIdent id : ids) {
                    if (++count % 1000 == 0) {
                        log.info("Memory migrate: still going...");
                    }
                    List<MemoryRecord> recs = loadMemoryOld(id.type, id.itemId);
                    List<com.threerings.msoy.room.data.EntityMemoryEntry> entries =
                        Lists.newArrayListWithExpectedSize(recs.size());
                    for (MemoryRecord rec : recs) {
                        entries.add(rec.toEntry());
                    }
                    storeMemories(new MemoriesRecord(entries));
                }
                log.info("Memory migrate: done!");
            }
        });
    }

    /**
     * Loads the memory for the specified item.
     */
    public MemoriesRecord loadMemory (byte itemType, int itemId)
    {
        return load(MemoriesRecord.class, MemoriesRecord.getKey(itemType, itemId));
    }

    /**
     * Loads up the all memory records for all items with the specified type and ids.
     */
    public List<MemoriesRecord> loadMemories (byte itemType, Collection<Integer> itemIds)
    {
        List<Key<MemoriesRecord>> keys = Lists.newArrayList();
        for (int itemId : itemIds) {
            keys.add(MemoriesRecord.getKey(itemType, itemId));
        }
        return loadAll(keys);
    }

    /**
     * Loads up the all memory records for all items with the specified type and ids.
     */
    public List<MemoriesRecord> loadMemories (Collection<ItemIdent> idents)
    {
        List<Key<MemoriesRecord>> keys = Lists.newArrayList();
        for (ItemIdent ident : idents) {
            keys.add(MemoriesRecord.getKey(ident.type, ident.itemId));
        }
        return loadAll(keys);
    }

    /**
     * Stores all supplied memory records.
     */
    public void storeMemories (Collection<MemoriesRecord> records)
    {
        for (MemoriesRecord record : records) {
            storeMemories(record);
        }
    }

    /**
     * Stores the memories for a particular entity in the repository.
     */
    public void storeMemories (MemoriesRecord record)
    {
        if (record.data == null) {
            delete(record);

        } else if (update(record) == 0) {
            insert(record);
        }
    }

    /**
     * Deletes all memories for the specified item.
     */
    public void deleteMemories (byte itemType, int itemId)
    {
        delete(MemoriesRecord.class, MemoriesRecord.getKey(itemType, itemId));
    }

    // TODO: remove. Support for convertMemories migration
    protected List<MemoryRecord> loadMemoryOld (byte itemType, int itemId)
    {
        return findAll(MemoryRecord.class, CacheStrategy.RECORDS, Lists.newArrayList(
            new Where(MemoryRecord.ITEM_TYPE, itemType, MemoryRecord.ITEM_ID, itemId)));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemoryRecord.class);
        classes.add(MemoriesRecord.class);
    }
}
