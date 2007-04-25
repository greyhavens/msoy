
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class EffectData extends FurniData
{
    /** The layer upon which the effect should reside. @see RoomCodes. */
    public var roomLayer :int;

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeByte(roomLayer);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        roomLayer = ins.readByte();
    }
}   
}
