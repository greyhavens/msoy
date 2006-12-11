//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.UniqueConstraint;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.GroupName;

/**
 * Contains the details of person's membership in a group.
 */
@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={
    GroupMembershipRecord.MEMBER_ID, GroupMembershipRecord.GROUP_ID }))
public class GroupMembershipRecord
    implements Cloneable
{
    public static final int SCHEMA_VERSION = 2;

    public static final String MEMBER_ID = "memberId";
    public static final String GROUP_ID = "groupId";
    public static final String RANK = "rank";

    /** The id of the member in the group membership. */
    public int memberId;

    /** The id of the group in the group membership. */ 
    public int groupId;
    
    /** The rank of the member in the group, defined in {@link GroupMembership}. */
    public byte rank;

    /** The date that this rank was assigned on.  Used to decide rank seniority. */
    public Timestamp rankAssigned;

    /** 
     * Converts this persistent record to a runtime record.
     */
    public GroupMembership toGroupMembership (GroupRecord group, MemberName member)
    {
        GroupMembership gm = new GroupMembership();
        gm.member = member;
        gm.group = new GroupName();
        gm.group.groupId = groupId;
        gm.group.groupName = group.name;
        gm.rank = rank;
        gm.rankAssignedDate = rankAssigned.getTime();
        return gm;
    }
    
    /**
     * Generates a string representation of this instance.
     */
    @Override
    public String toString ()
    {
        StringBuilder buf = new StringBuilder("[");
        StringUtil.fieldsToString(buf, this);
        return buf.append("]").toString();
    }
}
