//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.orth.data.MediaDesc;

public class PartyBoardInfo extends SimpleStreamableObject
{
    /** The immutable info. */
    public var summary :PartySummary;

    /** The mutable info. */
    public var info :PartyInfo;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        summary = PartySummary(ins.readObject());
        info = PartyInfo(ins.readObject());
    }
}
}
