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
    
    /** The public image URL for this badge. */
    public var imageUrl :String; 

    public function getKey () :Object
    {
        return badgeCode;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        badgeCode = ins.readInt();
        imageUrl = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(badgeCode);
        out.writeField(imageUrl);
    }
}

}
