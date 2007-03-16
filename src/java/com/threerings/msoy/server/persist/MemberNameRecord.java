//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * A computed persistent entity that's used to fetch (and cache) member name information only.
 */
@Computed(shadowOf=MemberRecord.class)
@Entity
public class MemberNameRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberNameRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(MemberNameRecord.class, NAME);
    // AUTO-GENERATED: FIELDS END
    
    /** This member's unique id. */
    @Id
    public int memberId;

    /** The name by which this member is known in MetaSOY. */
    public String name;
}
