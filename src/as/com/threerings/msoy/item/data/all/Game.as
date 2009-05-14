//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** A unique identifier assigned to this game. */
    public var gameId :int;

    /** True if this is an AVR game, false if it's a Parlor game. */
    public var isAVRG :Boolean;

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        gameId = ins.readInt();
        isAVRG = ins.readBoolean();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(gameId);
        out.writeBoolean(isAVRG);
    }
}
}
