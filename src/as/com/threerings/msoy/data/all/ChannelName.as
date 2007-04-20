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
    implements Comparable, Hashable
{
    /** The maximum length of a channel name */
    public static const LENGTH_MAX :int = 24;

    /** The minimum length of a channel name */
    public static const LENGTH_MIN :int = 3;

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

    // from Hashable (by way of Name)
    override public function hashCode () :int
    {
        return super.hashCode() ^ _creatorId;
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

    // from Equalable (by way of Hashable by way of Name)
    override public function equals (other :Object) :Boolean
    {
        if (other is ChannelName) {
            var oc :ChannelName = (other as ChannelName);
            return oc._creatorId == _creatorId && oc._name == _name;
        } else {
            return false;
        }
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

    /** The member id of this channel's creator. */
    protected var _creatorId :int;
}
}
