//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.all.PlayerEntry;
import com.threerings.msoy.data.all.VizMemberName;

/**
 * Represents a fellow party-goer connection.
 */
public class PartierEntry extends PlayerEntry
{
    /**
     * The order of the partier among all the players who have joined this party. The lower this
     * value, the better priority they have to be auto-assigned leadership.
     */
    public var joinOrder :int;

    override public function toString () :String
    {
        return "PartierEntry[" + name + "]";
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
