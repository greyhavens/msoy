//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class Audio extends Item
{
    /** The audio media. */
    public var audioMedia :MediaDesc;

    /** A description of this audio. */
    public var description :String;

    // from Item
    override public function getType () :int
    {
        return AUDIO;
    }

    // from Item
    override public function getDescription () :String
    {
        return description;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(audioMedia);
        out.writeField(description);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        audioMedia = (ins.readObject() as MediaDesc);
        description = (ins.readField(String) as String);
    }
}
}
