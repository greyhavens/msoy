//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.util.ComparisonChain;

import com.threerings.msoy.data.all.PlayerEntry;

/**
 * Represents a fellow party-goer connection.
 * Note that the DSet key is the player id.
 */
public class PartyPeep extends PlayerEntry
{
    /**
     * The order of the partier among all the players who have joined this party. The lower this
     * value, the better priority they have to be auto-assigned leadership.
     */
    public var joinOrder :int;

    /**
     * Create a sort function for sorting PartyPeep entries.
     * @param partyInfo an object that has a leaderId property.
     */
    public static function createSortByOrder (partyInfo :Object) :Function
    {
        return function (lhs :PartyPeep, rhs :PartyPeep, ... rest) :int {
            // always show the leader on top
            if (partyInfo.leaderId == lhs.name.getId()) {
                return -1;

            } else if (partyInfo.leaderId == rhs.name.getId()) {
                return 1;
            }
            return ComparisonChain.start()
                .compareInts(lhs.joinOrder, rhs.joinOrder)
                .compare(lhs, rhs, PlayerEntry.sortByName)
                .result();
        };
    }

    override public function toString () :String
    {
        return "PartyPeep[" + name + "]";
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        joinOrder = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeInt(joinOrder);
    }
}
}
