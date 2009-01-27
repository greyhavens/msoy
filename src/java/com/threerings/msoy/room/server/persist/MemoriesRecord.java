//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.room.data.EntityMemoryEntry;

import static com.threerings.msoy.Log.log;

/**
 * Maintains memory information for "smart" digital items (furniture, pets, etc).
 */
@Entity
public class MemoriesRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MemoriesRecord> _R = MemoriesRecord.class;
    public static final ColumnExp ITEM_TYPE = colexp(_R, "itemType");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp DATA = colexp(_R, "data");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The type of the item for which we're storing memory data. */
    @Id public byte itemType;

    /** The id of the item for which we're storing memory data. */
    @Id public int itemId;

    /** A serialized representation of all the memory data. */
    @Column(length=8192) // 4k + extra. See Ray for explanation.
    public byte[] data;

    /**
     * Extracts the modified memory entries from the supplied list and returns a list of
     * MemoriesRecord instances that can be saved to the database.
     */
    public static List<MemoriesRecord> extractModified (Iterable<EntityMemoryEntry> memories)
    {
        // TODO: I may change the runtime representation of memories if we keep this
        ListMultimap<ItemIdent, EntityMemoryEntry> map = Multimaps.newArrayListMultimap();
        Set<ItemIdent> modified = Sets.newHashSet();
        for (EntityMemoryEntry entry : memories) {
            map.put(entry.item, entry);
            if (entry.modified) {
                modified.add(entry.item);
            }
        }

        List<MemoriesRecord> mems = Lists.newArrayList();
        for (ItemIdent key : modified) {
            mems.add(new MemoriesRecord(map.get(key)));
        }
        return mems;
    }

    /** Used when loading instances from the repository. */
    public MemoriesRecord ()
    {
    }

    /**
     * Creates a memory record from the supplied memory information.
     * The specified entries should all refer to the same item.
     */
    public MemoriesRecord (Collection<EntityMemoryEntry> entries)
    {
        // first, assign our item id and prune any null entries
        for (Iterator<EntityMemoryEntry> itr = entries.iterator(); itr.hasNext(); ) {
            EntityMemoryEntry mem = itr.next();
            if (this.itemType == 0) {
                this.itemType = mem.item.type;
                this.itemId = mem.item.itemId;
            }
            if (mem.value == null) {
                itr.remove();
            }
        }

        if (entries.isEmpty()) {
            // this.data stays at null. We represent a loss of memory: delete the row.
            return;
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeShort(entries.size());
            for (EntityMemoryEntry mem : entries) {
                out.writeUTF(mem.key);
                out.writeShort(mem.value.length);
                out.write(mem.value);
            }
            out.flush();
            this.data = bos.toByteArray();
            // TEMP: debugging
            if (this.data.length > 4096) {
                log.warning("Encoded fat memories",
                    "itemType", itemType, "itemId", itemId, "length", this.data.length);
            }

        } catch (IOException ioe) {
            throw new RuntimeException("Can't happen", ioe);
        }
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public List<EntityMemoryEntry> toEntries ()
    {
        try {
            ItemIdent ident = new ItemIdent(this.itemType, this.itemId);
            DataInputStream ins = new DataInputStream(new ByteArrayInputStream(this.data));
            int count = ins.readShort();
            List<EntityMemoryEntry> entries = Lists.newArrayListWithExpectedSize(count);
            for (int ii = 0; ii < count; ii++) {
                EntityMemoryEntry entry = new EntityMemoryEntry();
                entry.item = ident;
                entry.key = ins.readUTF();
                entry.value = new byte[ins.readShort()];
                ins.read(entry.value, 0, entry.value.length);
                entries.add(entry);
            }
            // TEMP: debugging
            if (ins.available() > 0) {
                log.warning("Haven't fully read memories",
                    "ident", ident, "available", ins.available());
            }
            return entries;

        } catch (IOException ioe) {
            throw new RuntimeException("Can't happen", ioe);
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemoriesRecord}
     * with the supplied key values.
     */
    public static Key<MemoriesRecord> getKey (byte itemType, int itemId)
    {
        return new Key<MemoriesRecord>(
                MemoriesRecord.class,
                new ColumnExp[] { ITEM_TYPE, ITEM_ID },
                new Comparable[] { itemType, itemId });
    }
    // AUTO-GENERATED: METHODS END
}
