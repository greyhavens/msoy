//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.presents.dobj.DSet;

/**
 * Summarizes a person's membership in a group.
 */
public class GroupMembership
    implements Streamable, IsSerializable, DSet.Entry
{
    /** Not ever stored in a GroupMembership record, but useful for methods
     * that return a user's rank as a byte. */
    public static final byte RANK_NON_MEMBER = 0;

    /** Membership ranks. */
    public static final byte RANK_MEMBER = 1;
    public static final byte RANK_MANAGER = 2;
    
    /** The name and id of the member of the group. <em>Note:</em> this will be null in the records
     * maintained in a member's MemberObject. */
    public MemberName member;

    /** The group's id. */
    public int groupId;

    /** The group's name. */
    public String groupName;

    /** The member's rank in the group. */
    public byte rank; 

    // from DSet.Entry
    public Comparable getKey ()
    {
        // autoboxing makes GWT angry.  
        return new Integer(groupId);
    }
}
