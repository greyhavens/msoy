//
// $Id$

package com.threerings.msoy.web.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Comparable;
import com.threerings.util.Hashable;

public class GroupName
    implements Streamable, Comparable, Hashable
{
    /** the group's name. */
    public var groupName :String;

    /** the group's id. */
    public var groupId :int;

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        groupId = ins.readInt();
        groupName = (ins.readField(String) as String);
    }

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error("abstract");
//        out.writeInt(groupId);
//        out.writeField(groupName);
    }

    // from Comparable
    public function compareTo (other :Object) :int
    {
        var that :GroupName = (other as GroupName);
        return this.groupId - that.groupId;
    }

    // from Hashable
    public function hashCode () :int
    {
        return groupId;
    }

    // from Equalable (by way of Hashable)
    public function equals (other :Object) :Boolean
    {
        return (other is GroupName) &&
            ((other as GroupName).groupId == groupId);
    }
}
}
