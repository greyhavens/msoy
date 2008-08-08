//
// $Id$

package com.threerings.msoy.badge.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.badge.data.all.EarnedBadge;

public class EarnedBadgeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(EarnedBadgeRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #badgeCode} field. */
    public static final String BADGE_CODE = "badgeCode";

    /** The qualified column identifier for the {@link #badgeCode} field. */
    public static final ColumnExp BADGE_CODE_C =
        new ColumnExp(EarnedBadgeRecord.class, BADGE_CODE);

    /** The column identifier for the {@link #level} field. */
    public static final String LEVEL = "level";

    /** The qualified column identifier for the {@link #level} field. */
    public static final ColumnExp LEVEL_C =
        new ColumnExp(EarnedBadgeRecord.class, LEVEL);

    /** The column identifier for the {@link #whenEarned} field. */
    public static final String WHEN_EARNED = "whenEarned";

    /** The qualified column identifier for the {@link #whenEarned} field. */
    public static final ColumnExp WHEN_EARNED_C =
        new ColumnExp(EarnedBadgeRecord.class, WHEN_EARNED);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The id of the member that holds this badge. */
    @Id
    public int memberId;

    /** The code that uniquely identifies the badge type. */
    @Id
    public int badgeCode;

    /** The highest badge level that the player has attained. */
    public int level;

    /** The date and time when this badge was earned. */
    public Timestamp whenEarned;

    /**
     * Converts this persistent record to a runtime record.
     */
    public EarnedBadge toBadge ()
    {
        return new EarnedBadge(badgeCode, level, whenEarned.getTime());
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #EarnedBadgeRecord}
     * with the supplied key values.
     */
    public static Key<EarnedBadgeRecord> getKey (int memberId, int badgeCode)
    {
        return new Key<EarnedBadgeRecord>(
                EarnedBadgeRecord.class,
                new String[] { MEMBER_ID, BADGE_CODE },
                new Comparable[] { memberId, badgeCode });
    }
    // AUTO-GENERATED: METHODS END
}
