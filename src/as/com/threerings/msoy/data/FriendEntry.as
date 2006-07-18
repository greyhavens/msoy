package com.threerings.msoy.data {

import com.threerings.util.Comparable;
import com.threerings.util.Name;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DSet_Entry;

public class FriendEntry
    implements Comparable, DSet_Entry
{
    /** The name of this friend. */
    public var name :Name;

    /** Is the friend online? */
    public var online :Boolean;

    /** Mr. Constructor. */
    public function FriendEntry (name :Name = null, online :Boolean = false)
    {
        this.name = name;
        this.online = online;
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return name;
    }

    // from interface Comparable
    public function compareTo (other :Object) :int
    {
        var that :FriendEntry = (other as FriendEntry);
        // online folks show up above offline folks
        if (this.online != that.online) {
            return this.online ? -1 : 1;
        }
        // then, sort by name
        return this.name.compareTo(that.name);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(name);
        out.writeBoolean(online);
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        name = (ins.readObject() as Name);
        online = ins.readBoolean();
    }
}
}
