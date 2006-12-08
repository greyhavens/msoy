//
// $Id$

package com.threerings.msoy.web.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Comparable;

public class GroupName
    implements Streamable, Comparable
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
}
}
