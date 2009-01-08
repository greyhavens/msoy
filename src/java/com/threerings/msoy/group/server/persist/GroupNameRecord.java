//
// $Id$

package com.threerings.msoy.group.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.GroupName;

/**
 * A computed persistent entity that's used to fetch (and cache) group name information only.
 */
@Computed(shadowOf=GroupRecord.class)
@Entity
public class GroupNameRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GroupNameRecord> _R = GroupNameRecord.class;
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp NAME = colexp(_R, "name");
    // AUTO-GENERATED: FIELDS END

    /** The group's unique id. */
    @Id public int groupId;

    /** The group's name. */
    public String name;

    /**
     * Creates a runtime record from this persistent record.
     */
    public GroupName toGroupName ()
    {
        return new GroupName(name, groupId);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GroupNameRecord}
     * with the supplied key values.
     */
    public static Key<GroupNameRecord> getKey (int groupId)
    {
        return new Key<GroupNameRecord>(
                GroupNameRecord.class,
                new ColumnExp[] { GROUP_ID },
                new Comparable[] { groupId });
    }
    // AUTO-GENERATED: METHODS END
}
