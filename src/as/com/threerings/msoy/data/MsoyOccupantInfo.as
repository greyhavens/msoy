//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.data.OccupantInfo;

public class MsoyOccupantInfo extends OccupantInfo
{
    /** The media that represents our avatar. */
    public var avatar :MediaData;

    // documentation inherited
    public override function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(avatar);
    }

    // documentation inherited
    public override function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        avatar = (ins.readObject() as MediaData);
    }
}
}
