//
// $Id$

package com.threerings.msoy.group.data.all {

import com.threerings.util.Enum;

/**
 * Enumeration for the ranks that a group member can hold.
 */
public class GroupMembership_Rank extends Enum
{
    /** Unused rank code. This is not ever stored in a GroupMembership record, but is useful
     * for methods that return a user's rank as a byte. */
    public static const NON_MEMBER :GroupMembership_Rank = new GroupMembership_Rank("NON_MEMBER");

    /** Rank code for a member. */
    public static const MEMBER :GroupMembership_Rank = new GroupMembership_Rank("MEMBER");

    /** Rank code for a manager. */
    public static const MANAGER :GroupMembership_Rank = new GroupMembership_Rank("MANAGER");

    /** @private this is an enum */
    public function GroupMembership_Rank (name :String)
    {
        super(name);
    }

    /**
     * Compare this rank to another. Returns a negative number if this rank is lower than the
     * other, a positive number if higher, or zero if they are the same.
     */
    public function compare (other :GroupMembership_Rank) :int
    {
        return ordinal() - other.ordinal();
    }
}

}
