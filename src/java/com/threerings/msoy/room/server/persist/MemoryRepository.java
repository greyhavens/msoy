//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.impl.Modifier;

import com.samskivert.jdbc.DatabaseLiaison;

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

        registerMigration(new DataMigration("2009_02_03_dropMemoryRecord") {
            @Override public void invoke () throws DatabaseException {
                _ctx.invoke(new Modifier() {
                    @Override protected int invoke (Connection conn, DatabaseLiaison liaison)
                        throws SQLException {
                        return liaison.dropTable(conn, "MemoryRecord") ? 1 : 0;
                    }
                });
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

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemoriesRecord.class);
    }
}
