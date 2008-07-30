//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DSet;

/**
 * Contains a single state datum for a member for a game.
 */
public class GameState
    implements DSet.Entry, Comparable<GameState>
{
    /** The key for this state datum. */
    public String key;

    /** The actual contents of the state datum. */
    public byte[] value;

    /** Whether or not this datum will be persistently stored between sessions. */
    public boolean persistent;

    /** Used to track whether the memory is modified and should be flushed when the game is
     * unloaded. */
    public transient boolean modified;

    public GameState ()
    {
    }

    public GameState (String key, byte[] value, boolean persistent)
    {
        this.key = key;
        this.value = value;
        this.persistent = persistent;
    }

    /**
     * Returns the size in bytes of this entry's key and value. The key size assumes one byte per
     * character which is an approximation, but is fine for our memory size limitation purposes.
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

    // from interface Comparable<MemoryEntry>
    public int compareTo (GameState other)
    {
        return key.compareTo(other.key);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return other != null && other.getClass().equals(getClass()) &&
            key.equals(((GameState)other).key);
    }

    @Override // from Object
    public int hashCode ()
    {
        return key.hashCode();
    }
}
