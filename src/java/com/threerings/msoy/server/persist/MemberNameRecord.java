//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MemberName;

/**
 * A computed persistent entity that's used to fetch (and cache) member name information only.
 */
@Computed(shadowOf=MemberRecord.class)
@Entity
public class MemberNameRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MemberNameRecord> _R = MemberNameRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp ACCOUNT_NAME = colexp(_R, "accountName");
    public static final ColumnExp NAME = colexp(_R, "name");
    // AUTO-GENERATED: FIELDS END

    /** This member's unique id. */
    @Id
    public int memberId;

    /** The authentication account associated with this member. */
    public String accountName;

    /** The name by which this member is known in MetaSOY. */
    public String name;

    /**
     * Creates a runtime record from this persistent record.
     */
    public MemberName toMemberName ()
    {
        return new MemberName(name, memberId);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemberNameRecord}
     * with the supplied key values.
     */
    public static Key<MemberNameRecord> getKey (int memberId)
    {
        return new Key<MemberNameRecord>(
                MemberNameRecord.class,
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
