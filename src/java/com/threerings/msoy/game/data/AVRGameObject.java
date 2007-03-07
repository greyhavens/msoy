//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.world.data.MemoryEntry;

/**
 * A game config for an AVR game.
 */
public class AVRGameObject extends MsoyGameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memories</code> field. */
    public static final String MEMORIES = "memories";
    // AUTO-GENERATED: FIELDS END

    /** Contains the game's memories. */
    public DSet<MemoryEntry> memories = new DSet<MemoryEntry>();

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>memories</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMemories (MemoryEntry elem)
    {
        requestEntryAdd(MEMORIES, memories, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memories</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMemories (Comparable key)
    {
        requestEntryRemove(MEMORIES, memories, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memories</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMemories (MemoryEntry elem)
    {
        requestEntryUpdate(MEMORIES, memories, elem);
    }

    /**
     * Requests that the <code>memories</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMemories (DSet<com.threerings.msoy.world.data.MemoryEntry> value)
    {
        requestAttributeChange(MEMORIES, value, this.memories);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.world.data.MemoryEntry> clone =
            (value == null) ? null : value.typedClone();
        this.memories = clone;
    }
    // AUTO-GENERATED: METHODS END
}
