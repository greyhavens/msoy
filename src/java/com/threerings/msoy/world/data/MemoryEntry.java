//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains a single memory datum for a scene entity.
 */
public class MemoryEntry
    implements DSet.Entry, Comparable<MemoryEntry>
{
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
        return (value == null) ? 0 : (key.length() + value.length);
    }

    /**
     * Get a special key for use when removing this entry from a DSet.  Maybe this is early
     * optimization, but I hate that otherwise we're sending down the entire entry when we want to
     * remove it!
     */
    public Comparable getRemoveKey ()
    {
        MemoryEntry other = new MemoryEntry();
        other.item = this.item;
        other.key = this.key;
        // other.value == null
        return other;
    }

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return this;
    }

    // from interface Comparable<MemoryEntry>
    public int compareTo (MemoryEntry other)
    {
        int rv = item.compareTo(other.item);
        return (rv != 0) ? rv : key.compareTo(other.key);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        // equality is determined by key only
        MemoryEntry oentry = (MemoryEntry)other;
        return item.equals(oentry.item) && key.equals(oentry.key);
    }

    @Override // from Object
    public int hashCode ()
    {
        return item.hashCode() ^ key.hashCode();
    }
}
