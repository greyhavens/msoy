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

    /** The group's id. */
    public var groupId :int;

    /** The group's name. */
    public var groupName :String;

    /** The member's rank in the group. */
    public var rank :int; 

    // from DSet_Entry
    public function getKey () :Object
    {
        return groupId;
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        member = (ins.readObject() as MemberName);
        groupId = ins.readInt();
        groupName = (ins.readField(String) as String);
        rank = ins.readByte();
    }

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error("abstract");
//        out.writeObject(member);
//        out.writeInt(groupId);
//        out.writeField(groupName);
//        out.writeByte(rank);
    }
}
