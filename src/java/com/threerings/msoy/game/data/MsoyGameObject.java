//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.OccupantInfo;

import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;

/**
 * Extends Whirled game stuff with party awareness.
 */
public class MsoyGameObject extends WhirledGameObject
    implements PartyPlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>parties</code> field. */
    public static final String PARTIES = "parties";
    // AUTO-GENERATED: FIELDS END

    /** Information on the parties presently in this game. */
    public DSet<PartySummary> parties = DSet.newDSet();

    // from PartyPlaceObject
    public DSet<PartySummary> getParties ()
    {
        return parties;
    }

    // from PartyPlaceObject
    public DSet<OccupantInfo> getOccupants ()
    {
        return occupantInfo;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>parties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToParties (PartySummary elem)
    {
        requestEntryAdd(PARTIES, parties, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>parties</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromParties (Comparable<?> key)
    {
        requestEntryRemove(PARTIES, parties, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>parties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateParties (PartySummary elem)
    {
        requestEntryUpdate(PARTIES, parties, elem);
    }

    /**
     * Requests that the <code>parties</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setParties (DSet<PartySummary> value)
    {
        requestAttributeChange(PARTIES, value, this.parties);
        DSet<PartySummary> clone = (value == null) ? null : value.typedClone();
        this.parties = clone;
    }
    // AUTO-GENERATED: METHODS END
}
