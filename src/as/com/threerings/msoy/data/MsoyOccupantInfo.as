//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.data.OccupantInfo;

public class MsoyOccupantInfo extends OccupantInfo
{
    /** The media that represents our avatar. */
    public var media :MediaData;

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(media);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        media = (ins.readObject() as MediaData);
    }
}
}
