//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

/**
 * A reference to an AVRG for control purposes.
 */
public class ControllableAVRGame extends Controllable
{
    public function ControllableAVRGame (gameId :int = 0)
    {
        _gameId = gameId;
    }

    override public function getKey () :Object
    {
        return gameId;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _gameId = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(_gameId);
    }

    protected var _gameId :int;
}
}
