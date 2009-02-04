//
// $Id$

package com.threerings.msoy.group.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.data.all.GroupName;

/**
 * Summarizes a person's membership in a group.
 */
public class GroupMembership
    implements Streamable, DSet_Entry
{
    /** Unused rank code. This is not ever stored in a GroupMembership record, but is useful for
     * methods that return a user's rank as a byte. */
    public static const RANK_NON_MEMBER :int = 0;

    /** Rank code for a member. */
    public static const RANK_MEMBER :int = 1;

    /** Rank code for a manager. */
    public static const RANK_MANAGER :int = 2;

    /** The group's identity. */
    public var group :GroupName;

    /** The member's rank in the group. */
    public var rank :int;

    /**
     * Returns true if the supplied rank is a valid rank (not {@link #RANK_NON_MEMBER} or an
     * otherwise invalid number.
     */
    public static function isValidRank (rank :int) :Boolean
    {
        return rank >= RANK_MEMBER && rank <= RANK_MANAGER;
    }

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
        rank = ins.readByte();
    }

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(group);
        out.writeByte(rank);
    }
}
}
