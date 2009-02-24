//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.MediaDesc;

public class PartyBoardInfo extends SimpleStreamableObject
{
    public var info :PartyInfo;

    public var icon :MediaDesc;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        info = PartyInfo(ins.readObject());
        icon = MediaDesc(ins.readObject());
    }
}
}
