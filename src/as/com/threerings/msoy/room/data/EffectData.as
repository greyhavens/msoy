//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class EffectData extends FurniData
{
    /** The parameter should not be adjusted. */
    public static const MODE_NONE :int = 0;

    /** The parameter should be i18n translated. */
    public static const MODE_XLATE :int = 1;

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
