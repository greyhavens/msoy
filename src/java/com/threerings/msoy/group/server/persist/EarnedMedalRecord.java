//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

@Entity
public class EarnedMedalRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<EarnedMedalRecord> _R = EarnedMedalRecord.class;
    public static final ColumnExp<Integer> MEDAL_ID = colexp(_R, "medalId");
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Timestamp> WHEN_EARNED = colexp(_R, "whenEarned");
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
        return newKey(_R, medalId, memberId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEDAL_ID, MEMBER_ID); }
    // AUTO-GENERATED: METHODS END
}
