//
// $Id$

package com.threerings.msoy.world.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.world.data.MemoryEntry;

/**
 * Maintains memory information for "smart" digital items (furniture, pets, etc).
 */
@Entity
public class MemoryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #itemType} field. */
    public static final String ITEM_TYPE = "itemType";

    /** The qualified column identifier for the {@link #itemType} field. */
    public static final ColumnExp ITEM_TYPE_C =
        new ColumnExp(MemoryRecord.class, ITEM_TYPE);

    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(MemoryRecord.class, ITEM_ID);

    /** The column identifier for the {@link #datumKey} field. */
    public static final String DATUM_KEY = "datumKey";

    /** The qualified column identifier for the {@link #datumKey} field. */
    public static final ColumnExp DATUM_KEY_C =
        new ColumnExp(MemoryRecord.class, DATUM_KEY);

    /** The column identifier for the {@link #datumValue} field. */
    public static final String DATUM_VALUE = "datumValue";

    /** The qualified column identifier for the {@link #datumValue} field. */
    public static final ColumnExp DATUM_VALUE_C =
        new ColumnExp(MemoryRecord.class, DATUM_VALUE);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The type of the item for which we're storing a memory datum. */
    @Id public byte itemType;

    /** The id of the item for which we're storing a memory datum. */
    @Id public int itemId;

    /** The key that identifies this memory datum. */
    @Id public String datumKey;

    /** A serialized representation of this datum's value. */
    public byte[] datumValue;

    /** Used when loading instances from the repository. */
    public MemoryRecord ()
    {
    }

    /**
     * Creates a memory record from the supplied memory information.
     */
    public MemoryRecord (MemoryEntry entry)
    {
        this.itemType = entry.item.type;
        this.itemId = entry.item.itemId;
        this.datumKey = entry.key;
        this.datumValue = entry.value;
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public MemoryEntry toEntry ()
    {
        MemoryEntry entry = new MemoryEntry();
        entry.item = new ItemIdent(itemType, itemId);
        entry.key = datumKey;
        entry.value = datumValue;
        return entry;
    }
}
