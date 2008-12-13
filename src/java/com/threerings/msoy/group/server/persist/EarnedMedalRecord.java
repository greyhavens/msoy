//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.google.common.base.Function;

@Entity
public class EarnedMedalRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #medalId} field. */
    public static final String MEDAL_ID = "medalId";

    /** The qualified column identifier for the {@link #medalId} field. */
    public static final ColumnExp MEDAL_ID_C =
        new ColumnExp(EarnedMedalRecord.class, MEDAL_ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(EarnedMedalRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #whenEarned} field. */
    public static final String WHEN_EARNED = "whenEarned";

    /** The qualified column identifier for the {@link #whenEarned} field. */
    public static final ColumnExp WHEN_EARNED_C =
        new ColumnExp(EarnedMedalRecord.class, WHEN_EARNED);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** A function to convert an EarnedMedalRecord into an Integer representing the memberId that
     * earned it */
    public static final Function<EarnedMedalRecord, Integer> TO_MEMBER_ID =
        new Function<EarnedMedalRecord, Integer>() {
            public Integer apply (EarnedMedalRecord earnedMedalRec) {
                return earnedMedalRec.memberId;
            }
        };

    /** The unique id of the medal that was earned. */
    @Id public int medalId;

    /** The unique member id of the member that earned the medal. */
    @Id public int memberId;

    /** The time when the medal was earned. */
    public Timestamp whenEarned;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link EarnedMedalRecord}
     * with the supplied key values.
     */
    public static Key<EarnedMedalRecord> getKey (int medalId, int memberId)
    {
        return new Key<EarnedMedalRecord>(
                EarnedMedalRecord.class,
                new String[] { MEDAL_ID, MEMBER_ID },
                new Comparable[] { medalId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
