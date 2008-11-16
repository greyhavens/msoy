//
// $Id$

package com.threerings.msoy.badge.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.badge.data.BadgeType;
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

    /** Transforms a persistent record to a runtime record. */
    public static Function<EarnedBadgeRecord, EarnedBadge> TO_BADGE =
        new Function<EarnedBadgeRecord, EarnedBadge>() {
        public EarnedBadge apply (EarnedBadgeRecord record) {
            return record.toBadge();
        }
    };

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
     * Constructs an empty EarnedBadgeRecord.
     */
    public EarnedBadgeRecord ()
    {
    }

    /**
     * Constructs an EarnedBadgeRecord from an EarnedBadge.
     */
    public EarnedBadgeRecord (int memberId, EarnedBadge badge)
    {
        this.memberId = memberId;
        this.badgeCode = badge.badgeCode;
        this.level = badge.level;
        this.whenEarned = new Timestamp(badge.whenEarned);
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public EarnedBadge toBadge ()
    {
        BadgeType type = BadgeType.getType(badgeCode);
        String levelUnits = type.getRequiredUnitsString(level);
        int coinValue = type.getCoinValue(level);
        return new EarnedBadge(badgeCode, level, levelUnits, coinValue, whenEarned.getTime());
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /** Helper function for {@link #toString}. */
    public String badgeCodeToString ()
    {
        return String.valueOf(BadgeType.getType(badgeCode));
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link EarnedBadgeRecord}
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
