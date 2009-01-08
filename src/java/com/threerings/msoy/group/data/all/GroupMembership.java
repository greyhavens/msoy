//
// $Id$

package com.threerings.msoy.group.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.all.GroupName;

/**
 * Summarizes a person's membership in a group.
 */
public class GroupMembership
    implements Streamable, IsSerializable, DSet.Entry
{
    /** Unused rank code. This is not ever stored in a GroupMembership record, but is useful for
     * methods that return a user's rank as a byte. */
    public static final byte RANK_NON_MEMBER = 0;

    /** Rank code for a member. */
    public static final byte RANK_MEMBER = 1;

    /** Rank code for a manager. */
    public static final byte RANK_MANAGER = 2;

    /** The group's identity. */
    public GroupName group;

    /** This member's rank in the group. */
    public byte rank;

    /**
     * Returns true if the supplied rank is a valid rank (not {@link #RANK_NON_MEMBER} or an
     * otherwise invalid number.
     */
    public static boolean isValidRank (byte rank)
    {
        return rank >= RANK_MEMBER && rank <= RANK_MANAGER;
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return group.getGroupId();
    }
}
