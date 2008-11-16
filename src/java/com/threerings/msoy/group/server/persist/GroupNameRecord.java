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
    /** The column identifier for the {@link #groupId} field. */
    public static final String GROUP_ID = "groupId";

    /** The qualified column identifier for the {@link #groupId} field. */
    public static final ColumnExp GROUP_ID_C =
        new ColumnExp(GroupNameRecord.class, GROUP_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(GroupNameRecord.class, NAME);
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
                new String[] { GROUP_ID },
                new Comparable[] { groupId });
    }
    // AUTO-GENERATED: METHODS END
}
