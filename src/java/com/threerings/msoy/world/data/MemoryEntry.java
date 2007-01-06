//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.presents.dobj.DSet;

/**
 * Contains a single memory datum for a scene entity.
 */
public class MemoryEntry
    implements DSet.Entry, Comparable<MemoryEntry>
{
    /** The entity with which this memory is associated. */
    public EntityIdent entity;

    /** The key for this memory datum. */
    public String key;

    /** The actual contents of the memory datum. */
    public byte[] value;

    /** Used to track whether the memory is modified and should be flushed when the scene is
     * unloaded. */
    public transient boolean modified;

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return this;
    }

    // from interface Comparable<MemoryEntry>
    public int compareTo (MemoryEntry other)
    {
        int rv = entity.compareTo(other.entity);
        return (rv != 0) ? rv : key.compareTo(other.key);
    }
}
