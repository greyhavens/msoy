//
// $Id$

package com.threerings.msoy.world.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.world.data.MemoryEntry;

/**
 * Maintains memory information for "smart" digital items (furniture, pets, etc).
 */
@Entity
public class MemoryRecord
{
    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** Used when using {@link #itemType} in a query. */
    public static final String ITEM_TYPE = "itemType";

    /** Used when using {@link #itemId} in a query. */
    public static final String ITEM_ID = "itemId";

    /** The type of the item for which we're storing a memory datum. */
    @Id public byte itemType;

    /** The id of the item for which we're storing a memory datum. */
    @Id public int itemId;

    /** The key that identifies this memory datum. */
    @Id public String key;

    /** A serialized representation of this datum's value. */
    public byte[] value;

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
        this.key = entry.key;
        this.value = entry.value;
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public MemoryEntry toEntry ()
    {
        MemoryEntry entry = new MemoryEntry();
        entry.item = new ItemIdent(itemType, itemId);
        entry.key = key;
        entry.value = value;
        return entry;
    }
}
