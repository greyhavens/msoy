//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

public class PartyObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>mates</code> field. */
    public static final String MATES = "mates";

    /** The field name of the <code>partyId</code> field. */
    public static final String PARTY_ID = "partyId";

    /** The field name of the <code>name</code> field. */
    public static final String NAME = "name";
    // AUTO-GENERATED: FIELDS END

    public DSet<PartymateEntry> mates = new DSet<PartymateEntry>();

    public int partyId;

    public String name;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>mates</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMates (PartymateEntry elem)
    {
        requestEntryAdd(MATES, mates, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>mates</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMates (Comparable<?> key)
    {
        requestEntryRemove(MATES, mates, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>mates</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMates (PartymateEntry elem)
    {
        requestEntryUpdate(MATES, mates, elem);
    }

    /**
     * Requests that the <code>mates</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMates (DSet<PartymateEntry> value)
    {
        requestAttributeChange(MATES, value, this.mates);
        DSet<PartymateEntry> clone = (value == null) ? null : value.typedClone();
        this.mates = clone;
    }

    /**
     * Requests that the <code>partyId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPartyId (int value)
    {
        int ovalue = this.partyId;
        requestAttributeChange(
            PARTY_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.partyId = value;
    }

    /**
     * Requests that the <code>name</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setName (String value)
    {
        String ovalue = this.name;
        requestAttributeChange(
            NAME, value, ovalue);
        this.name = value;
    }
    // AUTO-GENERATED: METHODS END
}
