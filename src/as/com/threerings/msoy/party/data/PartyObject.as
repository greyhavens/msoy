//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class PartyObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>mates</code> field. */
    public static const MATES :String = "mates";
    // AUTO-GENERATED: FIELDS END

    public var mates :DSet;

    public var partyId :int;

    public var name :String;

//    // AUTO-GENERATED: METHODS START
//    /**
//     * Requests that the specified entry be added to the
//     * <code>mates</code> set. The set will not change until the event is
//     * actually propagated through the system.
//     */
//    public void addToMates (PartymateEntry elem)
//    {
//        requestEntryAdd(MATES, mates, elem);
//    }
//
//    /**
//     * Requests that the entry matching the supplied key be removed from
//     * the <code>mates</code> set. The set will not change until the
//     * event is actually propagated through the system.
//     */
//    public void removeFromMates (Comparable key)
//    {
//        requestEntryRemove(MATES, mates, key);
//    }

//    /**
//     * Requests that the specified entry be updated in the
//     * <code>mates</code> set. The set will not change until the event is
//     * actually propagated through the system.
//     */
//    public void updateMates (PartymateEntry elem)
//    {
//        requestEntryUpdate(MATES, mates, elem);
//    }

//    /**
//     * Requests that the <code>mates</code> field be set to the
//     * specified value. Generally one only adds, updates and removes
//     * entries of a distributed set, but certain situations call for a
//     * complete replacement of the set value. The local value will be
//     * updated immediately and an event will be propagated through the
//     * system to notify all listeners that the attribute did
//     * change. Proxied copies of this object (on clients) will apply the
//     * value change when they received the attribute changed notification.
//     */
//    public void setMates (DSet<PartymateEntry> value)
//    {
//        requestAttributeChange(MATES, value, this.mates);
//        @SuppressWarnings("unchecked") DSet<PartymateEntry> clone =
//            (value == null) ? null : value.typedClone();
//        this.mates = clone;
//    }
    // AUTO-GENERATED: METHODS END

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        mates = ins.readObject() as DSet;
        partyId = ins.readInt();
        name = ins.readField(String) as String;
    }
}
}
