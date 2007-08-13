//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.GroupName;

/**
 * Contains the details of person's membership in a group.
 */
@Entity(indices={
    @Index(name="ixGroup", columns={ GroupMembershipRecord.GROUP_ID })
})
public class GroupMembershipRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(GroupMembershipRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #groupId} field. */
    public static final String GROUP_ID = "groupId";

    /** The qualified column identifier for the {@link #groupId} field. */
    public static final ColumnExp GROUP_ID_C =
        new ColumnExp(GroupMembershipRecord.class, GROUP_ID);

    /** The column identifier for the {@link #rank} field. */
    public static final String RANK = "rank";

    /** The qualified column identifier for the {@link #rank} field. */
    public static final ColumnExp RANK_C =
        new ColumnExp(GroupMembershipRecord.class, RANK);

    /** The column identifier for the {@link #rankAssigned} field. */
    public static final String RANK_ASSIGNED = "rankAssigned";

    /** The qualified column identifier for the {@link #rankAssigned} field. */
    public static final ColumnExp RANK_ASSIGNED_C =
        new ColumnExp(GroupMembershipRecord.class, RANK_ASSIGNED);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The id of the member in the group membership. */
    @Id
    public int memberId;

    @Id
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
        gm.group = new GroupName(group.name, groupId);
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

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GroupMembershipRecord}
     * with the supplied key values.
     */
    public static Key<GroupMembershipRecord> getKey (int memberId, int groupId)
    {
        return new Key<GroupMembershipRecord>(
                GroupMembershipRecord.class,
                new String[] { MEMBER_ID, GROUP_ID },
                new Comparable[] { memberId, groupId });
    }
    // AUTO-GENERATED: METHODS END
}
