//
// $Id$

package com.threerings.msoy.badge.server.persist;

import com.google.common.base.Function;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.InProgressBadge;

public class InProgressBadgeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(InProgressBadgeRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #badgeCode} field. */
    public static final String BADGE_CODE = "badgeCode";

    /** The qualified column identifier for the {@link #badgeCode} field. */
    public static final ColumnExp BADGE_CODE_C =
        new ColumnExp(InProgressBadgeRecord.class, BADGE_CODE);

    /** The column identifier for the {@link #nextLevel} field. */
    public static final String NEXT_LEVEL = "nextLevel";

    /** The qualified column identifier for the {@link #nextLevel} field. */
    public static final ColumnExp NEXT_LEVEL_C =
        new ColumnExp(InProgressBadgeRecord.class, NEXT_LEVEL);

    /** The column identifier for the {@link #progress} field. */
    public static final String PROGRESS = "progress";

    /** The qualified column identifier for the {@link #progress} field. */
    public static final ColumnExp PROGRESS_C =
        new ColumnExp(InProgressBadgeRecord.class, PROGRESS);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** Before being stored in the database, InProgressBadgeRecords should have their progress
     * rounded down to a multiple of PROGRESS_INCREMENT (to prevent hammering the database with
     * insignificant badge progress updates). See {@link #quantizeProgress}. */
    public static final float PROGRESS_INCREMENT = 0.1f;

    /** Transforms a persistent record to a runtime record. */
    public static Function<InProgressBadgeRecord, InProgressBadge> TO_BADGE =
        new Function<InProgressBadgeRecord, InProgressBadge>() {
        public InProgressBadge apply (InProgressBadgeRecord record) {
            return record.toBadge();
        }
    };

    /** The id of the member that holds this badge. */
    @Id
    public int memberId;

    /** The code that uniquely identifies the badge type. */
    @Id
    public int badgeCode;

    /** The badge level that the player is currently working towards. */
    public int nextLevel;

    /** The progress that has been made on the badge, in [0, 1). */
    public float progress;

    /**
     * Constructs an empty InProgressBadgeRecord.
     */
    public InProgressBadgeRecord ()
    {
    }

    /**
     * Constructs an InProgressBadgeRecord from an InProgressBadge.
     */
    public InProgressBadgeRecord (int memberId, InProgressBadge badge)
    {
        this.memberId = memberId;
        this.badgeCode = badge.badgeCode;
        this.nextLevel = badge.nextLevel;
        this.progress = badge.progress;
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public InProgressBadge toBadge ()
    {
        return new InProgressBadge(badgeCode, nextLevel, progress);
    }

    public static float quantizeProgress (float progress)
    {
        return (float)(Math.floor(progress / PROGRESS_INCREMENT) * PROGRESS_INCREMENT);
    }

    /**
     * @return a String representation of the record.
     */
    @Override
    public String toString ()
    {
        return "memberId=" + memberId + " BadgeType=" + BadgeType.getType(badgeCode) +
            " nextLevel=" + nextLevel + " progress=" + progress;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #InProgressBadgeRecord}
     * with the supplied key values.
     */
    public static Key<InProgressBadgeRecord> getKey (int memberId, int badgeCode)
    {
        return new Key<InProgressBadgeRecord>(
                InProgressBadgeRecord.class,
                new String[] { MEMBER_ID, BADGE_CODE },
                new Comparable[] { memberId, badgeCode });
    }
    // AUTO-GENERATED: METHODS END
}
