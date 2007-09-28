//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

/**
 * A game config for an AVR game.
 */
public class AVRGameObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>state</code> field. */
    public static final String STATE = "state";
    // AUTO-GENERATED: FIELDS END

    /** Contains the game's memories. */
    public DSet<GameState> state = new DSet<GameState>();

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>state</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToState (GameState elem)
    {
        requestEntryAdd(STATE, state, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>state</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromState (Comparable key)
    {
        requestEntryRemove(STATE, state, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>state</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateState (GameState elem)
    {
        requestEntryUpdate(STATE, state, elem);
    }

    /**
     * Requests that the <code>state</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setState (DSet<com.threerings.msoy.game.data.GameState> value)
    {
        requestAttributeChange(STATE, value, this.state);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.game.data.GameState> clone =
            (value == null) ? null : value.typedClone();
        this.state = clone;
    }
    // AUTO-GENERATED: METHODS END
}
