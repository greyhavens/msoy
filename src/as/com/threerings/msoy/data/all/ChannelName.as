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
 * Contains a channel name and channel id in one handy object.
 */
public class ChannelName extends Name
{
    public function ChannelName (name :String = null, creatorId :int = 0)
    {
        super(name);
        _creatorId = creatorId;
    }

    /**
     * Returns the member id of the creator of this chat channel.
     */
    public function getCreatorId () :int
    {
        return _creatorId;
    }

    // from Streamable (by way of Name)
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _creatorId = ins.readInt();
    }

    // from Streamable (by way of Name)
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(_creatorId);
    }

    // from Comparable (by way of Name)
    override public function compareTo (other :Object) :int
    {
        var oc :ChannelName = (other as ChannelName);
        if (_creatorId == oc._creatorId) {
            return super.compareTo(oc);
        } else {
            return _creatorId - oc._creatorId;
        }
    }

    // from Hashable (by way of Name)
    override public function hashCode () :int
    {
        return super.hashCode() ^ _creatorId;
    }

    // from Equalable (by way of Hashable by way of Name)
    override public function equals (other :Object) :Boolean
    {
        if (other is ChannelName) {
            var oc :ChannelName = (other as ChannelName);
            return oc._creatorId == _creatorId && oc._name.equals(_name);
        } else {
            return false;
        }
    }

    /** The channel's id. */
    protected var _creatorId :int;
}
}
