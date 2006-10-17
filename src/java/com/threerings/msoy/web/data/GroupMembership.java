//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Summarizes a person's membership in a group.
 */
public class GroupMembership
    implements Streamable, IsSerializable
{
    public static final byte RANK_MEMBER = 1;
    public static final byte RANK_MANAGER = 2;
    
    /** The name and id of the member of the group. */
    public MemberGName member;

    /** The group's id. */
    public int groupId;

    /** The group's name. */
    public String groupName;

    /** The member's rank in the group. */
    public byte rank; 
}
