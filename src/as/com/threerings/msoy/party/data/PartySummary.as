//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;

public class PartySummary extends SimpleStreamableObject
    implements DSet_Entry
{
    /** The party id. */
    public var id :int;

    /** The current name of the party. */
    public var name :String;

    /** The name of the group (and id). */
    public var group :GroupName;

    /** The party's icon (the group icon). */
    public var icon :MediaDesc;

    // from DSet_Entry
    public function getKey () :Object
    {
        return id;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        id = ins.readInt();
        name = ins.readField(String) as String;
        group = GroupName(ins.readObject());
        icon = MediaDesc(ins.readObject());
    }
}
}
