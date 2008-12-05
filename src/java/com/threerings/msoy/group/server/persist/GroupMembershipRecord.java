//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;

import com.samskivert.util.IntMap;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.GroupName;

import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupMemberCard;

/**
 * Contains the details of person's membership in a group.
 */
@Entity(indices={
    @Index(name="ixGroup", fields={ GroupMembershipRecord.GROUP_ID })
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

    /** Converts a persistent record into a {@link GroupCard}. */
    public static final Function<GroupMembershipRecord, Integer> TO_GROUP_ID =
        new Function<GroupMembershipRecord, Integer>() {
        public Integer apply (GroupMembershipRecord record) {
            return record.groupId;
        }
    };

    public static final int SCHEMA_VERSION = 3;

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
    public GroupMembership toGroupMembership (IntMap<GroupName> groups)
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
        return new Key<GroupMembershipRecord>(
                GroupMembershipRecord.class,
                new String[] { MEMBER_ID, GROUP_ID },
                new Comparable[] { memberId, groupId });
    }
    // AUTO-GENERATED: METHODS END
}
