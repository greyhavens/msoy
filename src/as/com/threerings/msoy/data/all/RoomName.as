//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Comparable;
import com.threerings.util.Hashable;
import com.threerings.util.Name;

/**
 * Contains a room name and scene id in one handy object.
 */
public class RoomName extends Name
    implements Comparable, Hashable
{
    public function RoomName (name :String = null, sceneId :int = 0)
    {
        super(name);
        _sceneId = sceneId;
    }

    /**
     * Returns the id of this room.
     */
    public function getSceneId () :int
    {
        return _sceneId;
    }

    // from Hashable (by way of Name)
    override public function hashCode () :int
    {
        return _sceneId;
    }

    // from Comparable (by way of Name)
    override public function compareTo (other :Object) :int
    {
        var that :RoomName = (other as RoomName);
        return this._sceneId - that._sceneId;
    }

    // from Equalable (by way of Hashable by way of Name)
    override public function equals (other :Object) :Boolean
    {
        return (other is RoomName) && ((other as RoomName)._sceneId == _sceneId);
    }

    // from Streamable (by way of Name)
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _sceneId = ins.readInt();
    }

    // from Streamable (by way of Name)
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(_sceneId);
    }

    /** The room's id. */
    protected var _sceneId :int;
}
}
