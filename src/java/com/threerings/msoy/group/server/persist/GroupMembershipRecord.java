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
@Entity
public class GroupMembershipRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GroupMembershipRecord> _R = GroupMembershipRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp RANK = colexp(_R, "rank");
    public static final ColumnExp RANK_ASSIGNED = colexp(_R, "rankAssigned");
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
    @Id public int memberId;

    /** The id of the group in the group membership. */
    @Id @Index(name="ixGroup")
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
                new ColumnExp[] { MEMBER_ID, GROUP_ID },
                new Comparable[] { memberId, groupId });
    }
    // AUTO-GENERATED: METHODS END
}
