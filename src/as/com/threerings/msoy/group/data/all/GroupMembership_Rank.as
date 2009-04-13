//
// $Id$

package com.threerings.msoy.group.data.all {

import com.threerings.util.Enum;

/**
 * Enumeration for the ranks that a group member can hold.
 */
public final class GroupMembership_Rank extends Enum
{
    /** Unused rank code. This is not ever stored in a GroupMembership record, but is useful
     * for methods that return a user's rank as a byte. */
    public static const NON_MEMBER :GroupMembership_Rank = new GroupMembership_Rank("NON_MEMBER");

    /** Rank code for a member. */
    public static const MEMBER :GroupMembership_Rank = new GroupMembership_Rank("MEMBER");

    /** Rank code for a manager. */
    public static const MANAGER :GroupMembership_Rank = new GroupMembership_Rank("MANAGER");
    finishedEnumerating(GroupMembership_Rank);

    /** @private this is an enum */
    public function GroupMembership_Rank (name :String)
    {
        super(name);
    }

    // not done- not needed. See Enum.
    // TODO: public static function valueOf (name :String) :GroupMembership_RANK;
    // TODO: public static function values () :Array;
}

}
