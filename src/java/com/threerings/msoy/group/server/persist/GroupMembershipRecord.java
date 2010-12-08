//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.sql.Timestamp;
import java.util.Map;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.GroupName;

import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.gwt.GroupMemberCard;

/**
 * Contains the details of person's membership in a group.
 */
@Entity
public class GroupMembershipRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GroupMembershipRecord> _R = GroupMembershipRecord.class;
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Integer> GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp<GroupMembership.Rank> RANK = colexp(_R, "rank");
    public static final ColumnExp<Timestamp> RANK_ASSIGNED = colexp(_R, "rankAssigned");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    /** The id of the member in the group membership. */
    @Id public int memberId;

    /** The id of the group in the group membership. */
    @Id @Index(name="ixGroup")
    public int groupId;

    /** The rank of the member in the group. */
    public Rank rank;

    /** The date that this rank was assigned on.  Used to decide rank seniority. */
    public Timestamp rankAssigned;

    /**
     * Converts this persistent record to a group member card.
     */
    public GroupMemberCard toGroupMemberCard ()
    {
        GroupMemberCard card = new GroupMemberCard();
        card.rank = rank;
        card.rankAssigned = rankAssigned.getTime();
        return card;
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public GroupMembership toGroupMembership (Map<Integer, GroupName> groups)
    {
        GroupMembership gm = new GroupMembership();
        gm.rank = rank;
        gm.group = groups.get(groupId);
        return gm;
    }

    /**
     * Generates a string representation of this instance.
     */
    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GroupMembershipRecord}
     * with the supplied key values.
     */
    public static Key<GroupMembershipRecord> getKey (int memberId, int groupId)
    {
        return newKey(_R, memberId, groupId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID, GROUP_ID); }
    // AUTO-GENERATED: METHODS END
}
