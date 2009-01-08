//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;

import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;

import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains information on member warnings and temporary bans.
 */
public class MemberWarningRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MemberWarningRecord> _R = MemberWarningRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp WARNING = colexp(_R, "warning");
    public static final ColumnExp BAN_EXPIRES = colexp(_R, "banExpires");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 15;

    /** This member's unique id. */
    @Id
    public int memberId;

    /** The warning message. */
    public String warning;

    /** The time the user's temp ban will expire. */
    @Column(nullable=true)
    public Timestamp banExpires;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemberWarningRecord}
     * with the supplied key values.
     */
    public static Key<MemberWarningRecord> getKey (int memberId)
    {
        return new Key<MemberWarningRecord>(
                MemberWarningRecord.class,
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
