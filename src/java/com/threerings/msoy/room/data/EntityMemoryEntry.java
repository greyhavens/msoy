//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains a single memory datum for a scene entity.
 */
public class EntityMemoryEntry
    implements DSet.Entry, Comparable<EntityMemoryEntry>
{
    /** Maximum size for a memory. */
    // TODO: make sure this is used everywhere we try to save memories.
    public static final int MAX_ENCODED_PROPERTY_LENGTH = 16 * 256;

    /** The item with which this memory datum is associated. */
    public ItemIdent item;

    /** The key for this memory datum. */
    public String key;

    /** The actual contents of the memory datum. */
    public byte[] value;

    /** Used to track whether the memory is modified and should be flushed when the scene is
     * unloaded. */
    public transient boolean modified;

    /**
     * Returns the size in bytes of this entry's key and value. The key size assumes one byte per
     * character which is an approximation, but is fine for our memory size limitation purposes.
     */
    public int getSize ()
    {
        // null valued entries are scheduled for removal, so don't count their size at all
        return (value == null) ? 0 : (key.length() + value.length);
    }

    /**
     * Get a special key for use when removing this entry from a DSet.  Maybe this is early
     * optimization, but I hate that otherwise we're sending down the entire entry when we want to
     * remove it!
     */
    public Comparable<?> getRemoveKey ()
    {
        EntityMemoryEntry other = new EntityMemoryEntry();
        other.item = this.item;
        other.key = this.key;
        // other.value == null
        return other;
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return this;
    }

    // from interface Comparable<MemoryEntry>
    public int compareTo (EntityMemoryEntry other)
    {
        int rv = item.compareTo(other.item);
        return (rv != 0) ? rv : key.compareTo(other.key);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        // equality is determined by key only
        EntityMemoryEntry oentry = (EntityMemoryEntry)other;
        return item.equals(oentry.item) && key.equals(oentry.key);
    }

    @Override // from Object
    public int hashCode ()
    {
        return item.hashCode() ^ key.hashCode();
    }
}
