//
// $Id$

package com.threerings.msoy.room.data;

import java.util.Map;

import com.threerings.presents.dobj.DSet;

import com.threerings.util.StreamableHashMap;

import com.threerings.msoy.item.data.all.ItemIdent;

public class EntityMemories
    implements DSet.Entry
{
    public static final int MAX_ENCODED_MEMORY_LENGTH = 4096;

    /** The item with which these memories are associated. */
    public ItemIdent ident;

    /** The memory key/values. */
    public StreamableHashMap<String, byte[]> memories;

    /** Were these memories modified since being loaded from the database? */
    public transient boolean modified;

    /** Suitable for unserialization. */
    public EntityMemories ()
    {
    }

    /**
     * Initialize a new memories entry with one memory and mark it as modified.
     */
    public EntityMemories (ItemIdent ident, String key, byte[] value)
    {
        this.ident = ident;
        memories = new StreamableHashMap<String, byte[]>();
        memories.put(key, value);
        modified = true;
    }

    /**
     * Get the estimated size for all the entries present, excluding the specified key
     */
    public int getSize (String skipKey)
    {
        int size = 0;
        for (Map.Entry<String, byte[]> entry : memories.entrySet()) {
            String key = entry.getKey();
            if (!skipKey.equals(key)) {
                size += getSize(key, entry.getValue());
            }
        }
        return size;
    }

    public static int getSize (String key, byte[] value)
    {
        return (value == null) ? 0 : (key.length() + value.length);
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return ident;
    }

    @Override
    public String toString ()
    {
        return "[ident=" + ident + ", modified=" + modified + "]";
    }

    /**
     * Called by the MemoryChangedEvent to directly update a value already in the map.
     */
    protected void setMemory (String key, byte[] newValue)
    {
        if (newValue == null) {
            memories.remove(key);
        } else {
            memories.put(key, newValue);
        }
        modified = true;
    }
}
