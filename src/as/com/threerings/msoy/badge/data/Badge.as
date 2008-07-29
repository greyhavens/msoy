package com.threerings.msoy.badge.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet_Entry;

public class Badge extends SimpleStreamableObject
    implements DSet_Entry
{
    /** The code that uniquely identifies this badge type. */
    public var badgeCode :int;

    public function getKey () :Object
    {
        return badgeCode;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        badgeCode = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(badgeCode);
    }
}

}
