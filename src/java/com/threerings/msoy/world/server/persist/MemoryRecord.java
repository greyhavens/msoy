//
// $Id$

package com.threerings.msoy.world.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;

import com.threerings.msoy.world.data.MemoryEntry;

/**
 * Maintains memory information for "smart" digital items (furniture, pets, etc).
 */
@Entity
public class MemoryRecord
{
    /** Used when using {@link #memoryId} in a query. */
    public static final String MEMORY_ID = "memoryId";

    /** The entity-specific unique identifier for our entity. */
    @Id public int memoryId;

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
        this.memoryId = entry.memoryId;
        this.key = entry.key;
        this.value = entry.value;
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public MemoryEntry toEntry ()
    {
        MemoryEntry entry = new MemoryEntry();
        entry.memoryId = memoryId;
        entry.key = key;
        entry.value = value;
        return entry;
    }
}
