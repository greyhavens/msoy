//
// $Id$

package com.threerings.msoy.room.data {

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

    override public function equals (other :Object) :Boolean
    {
        return ((other is ControllableAVRGame) &&
                _gameId == (other as ControllableAVRGame).getGameId());
    }

    public function getGameId () :int
    {
        return _gameId;
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
