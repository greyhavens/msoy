//
// $Id$

package com.threerings.msoy.badge.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * A database record representing a badge that has been "suppressed" by the user.
 */
public class SuppressedBadgeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(SuppressedBadgeRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #badgeCode} field. */
    public static final String BADGE_CODE = "badgeCode";

    /** The qualified column identifier for the {@link #badgeCode} field. */
    public static final ColumnExp BADGE_CODE_C =
        new ColumnExp(SuppressedBadgeRecord.class, BADGE_CODE);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The id of the member that suppressed this badge. */
    public int memberId;

    /** The code that uniquely identifies the badge type. */
    public int badgeCode;
}
