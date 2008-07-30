//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.presents.dobj.DSet;

/**
 * Contains a single property for shared room state.
 */
public class RoomPropertyEntry
    implements DSet.Entry, Comparable<RoomPropertyEntry>
{
    public static final int MAX_ENTRIES = 16;
    public static final int MAX_KEY_LENGTH = 64;
    public static final int MAX_VALUE_LENGTH = 256;

    /** The key for this property. */
    public String key;

    /** The actual contents of the property. */
    public byte[] value;

    /**
     * Returns the size in bytes of this entry's key and value. The key size assumes one byte per
     * character which is an approximation, but is fine for our property size limitation purposes.
     */
    public int getSize ()
    {
        // null valued entries are scheduled for removal, so don't count their size at all
        return (value == null) ? 0 : (key.length() + value.length);
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return key;
    }

    // from interface Comparable<RoomPropertyEntry>
    public int compareTo (RoomPropertyEntry other)
    {
        return key.compareTo(other.key);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return key.equals(((RoomPropertyEntry)other).key);
    }

    @Override // from Object
    public int hashCode ()
    {
        return key.hashCode();
    }
}
