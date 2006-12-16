//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class Audio extends Item
{
    /** The audio media. */
    public var audioMedia :MediaDesc;

    // from Item
    override public function getType () :int
    {
        return AUDIO;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(audioMedia);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        audioMedia = (ins.readObject() as MediaDesc);
    }
}
}
