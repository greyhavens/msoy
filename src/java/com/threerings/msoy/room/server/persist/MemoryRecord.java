//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.room.data.EntityMemoryEntry;

/**
 * Maintains memory information for "smart" digital items (furniture, pets, etc).
 */
@Entity
public class MemoryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MemoryRecord> _R = MemoryRecord.class;
    public static final ColumnExp ITEM_TYPE = colexp(_R, "itemType");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp DATUM_KEY = colexp(_R, "datumKey");
    public static final ColumnExp DATUM_VALUE = colexp(_R, "datumValue");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** Transforms a persistent record to a runtime record. */
    public static Function<MemoryRecord, EntityMemoryEntry> TO_ENTRY =
        new Function<MemoryRecord, EntityMemoryEntry>() {
        public EntityMemoryEntry apply (MemoryRecord record) {
            return record.toEntry();
        }
    };

    /** The type of the item for which we're storing a memory datum. */
    @Id public byte itemType;

    /** The id of the item for which we're storing a memory datum. */
    @Id public int itemId;

    /** The key that identifies this memory datum. */
    @Id public String datumKey;

    /** A serialized representation of this datum's value. */
    @Column(length=4096)
    public byte[] datumValue;

    /**
     * Extracts the modified memory entries from the supplied list and returns a list of
     * MemoryRecord instances that can be saved to the database.
     */
    public static List<MemoryRecord> extractModified (Iterable<EntityMemoryEntry> memories)
    {
        List<MemoryRecord> memrecs = Lists.newArrayList();
        for (EntityMemoryEntry entry : memories) {
            if (entry.modified) {
                memrecs.add(new MemoryRecord(entry));
            }
        }
        return memrecs;
    }

    /** Used when loading instances from the repository. */
    public MemoryRecord ()
    {
    }

    /**
     * Creates a memory record from the supplied memory information.
     */
    public MemoryRecord (EntityMemoryEntry entry)
    {
        this.itemType = entry.item.type;
        this.itemId = entry.item.itemId;
        this.datumKey = entry.key;
        this.datumValue = entry.value;
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public EntityMemoryEntry toEntry ()
    {
        EntityMemoryEntry entry = new EntityMemoryEntry();
        entry.item = new ItemIdent(itemType, itemId);
        entry.key = datumKey;
        entry.value = datumValue;
        return entry;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemoryRecord}
     * with the supplied key values.
     */
    public static Key<MemoryRecord> getKey (byte itemType, int itemId, String datumKey)
    {
        return new Key<MemoryRecord>(
                MemoryRecord.class,
                new ColumnExp[] { ITEM_TYPE, ITEM_ID, DATUM_KEY },
                new Comparable[] { itemType, itemId, datumKey });
    }
    // AUTO-GENERATED: METHODS END
}
