//
// $Id$

package com.threerings.msoy.group.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.data.all.GroupName;

/**
 * Summarizes a person's membership in a group.
 */
public class GroupMembership
    implements Streamable, DSet_Entry
{
    /** The group's identity. */
    public var group :GroupName;

    /** The member's rank in the group. */
    public var rank :GroupMembership_Rank;

    /**
     * A sort function that may be used for GroupMemberships.
     */
    public static function sortByName (lhs :GroupMembership, rhs :GroupMembership, ... rest) :int
    {
        return GroupName.BY_DISPLAY_NAME(lhs.group, rhs.group);
    }

    public function GroupMembership ()
    {
    }

    // from DSet_Entry
    public function getKey () :Object
    {
        return group.getGroupId();
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        group = GroupName(ins.readObject());
        rank = GroupMembership_Rank(ins.readObject());
    }

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(group);
        out.writeObject(rank);
    }
}
}
