//
// $Id$

package com.threerings.msoy.room.data;

import java.util.Arrays;
import java.util.Map;

import com.threerings.presents.dobj.DSet;

import com.threerings.util.StreamableHashMap;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Holds the memories of an entity in a room.
 *
 * This class is cloneable so that we may clone the memories of a user when they leave the room
 * and we wish to leave their puppet behind with the latest updates.
 */
@com.threerings.util.ActionScript(omit=true)
public class EntityMemories
    implements DSet.Entry, Cloneable
{
    public static final int MAX_ENCODED_MEMORY_LENGTH = 4096;

    /** The item with which these memories are associated. */
    public ItemIdent ident;

    /** The memory key/values. */
    public StreamableHashMap<String, byte[]> memories;

    /** Were these memories modified since being loaded from the database? */
    public boolean modified;

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

    /**
     * Get the estimated size of one particular entry.
     */
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
        return "[ident=" + ident + ", size=" + memories.size() + ", modified=" + modified + "]";
    }

    @Override
    public EntityMemories clone ()
    {
        try {
            EntityMemories copy = (EntityMemories) super.clone();
            // mainly we need to clone the memories so that they can safely be modified in
            // another room on this same node
            @SuppressWarnings("unchecked")
            StreamableHashMap<String, byte[]> memcopy =
                (StreamableHashMap<String, byte[]>) copy.memories.clone();
            copy.memories = memcopy;
            return copy;

        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse); // shouldn't happen
        }
    }

    /**
     * Called by the MemoryChangedEvent to directly update a value already in the map.
     */
    protected void setMemory (String key, byte[] newValue)
    {
        byte[] oldValue = (newValue == null)
            ? memories.remove(key)
            : memories.put(key, newValue);
        modified = modified || !Arrays.equals(oldValue, newValue);
    }
}
