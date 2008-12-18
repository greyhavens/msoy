//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.TypedArray;

public class PartyDetail extends PartyBoardInfo
{
    public var groupName :String;

    public var peeps :TypedArray /* of PartyPeep */;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        groupName = ins.readField(String) as String;
        peeps = TypedArray(ins.readObject());
    }
}
}
