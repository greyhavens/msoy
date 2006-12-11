//
// $Id$

package com.threerings.msoy.web.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Summarizes a person's membership in a group.
 */
public class GroupMembership
    implements Streamable, DSet_Entry
{
    /** Not ever stored in a GroupMembership record, but useful for methods
     * that return a user's rank as a byte. */
    public static const RANK_NON_MEMBER :int = 0;

    /** Membership ranks. */
    public static const RANK_MEMBER :int = 1;
    public static const RANK_MANAGER :int = 2;
    
    /** The name and id of the member of the group. <em>Note:</em> this will be null in the records
     * maintained in a member's MemberObject. */
    public var member :MemberName;

    /** The group's identity. */
    public var group :GroupName;

    /** The member's rank in the group. */
    public var rank :int; 

    // from DSet_Entry
    public function getKey () :Object
    {
        return group;
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        member = (ins.readObject() as MemberName);
        group = (ins.readObject() as GroupName);
        rank = ins.readByte();
        // TODO: this hackery to discard the (long) rankAssigned field from the Java side should
        // become something more meaningful.
        ins.readInt();
        ins.readInt();
    }

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error("abstract");
//        out.writeObject(member);
//        out.writeObject(group);
//        out.writeByte(rank);
    }
}
}
