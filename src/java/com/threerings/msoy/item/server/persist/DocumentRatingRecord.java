//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.server.persist.RatingRecord;

/** Rating records for Documents. */
public class DocumentRatingRecord extends RatingRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<DocumentRatingRecord> _R = DocumentRatingRecord.class;
    public static final ColumnExp<Integer> TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Byte> RATING = colexp(_R, "rating");
    public static final ColumnExp<Timestamp> TIMESTAMP = colexp(_R, "timestamp");
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DocumentRatingRecord}
     * with the supplied key values.
     */
    public static Key<DocumentRatingRecord> getKey (int targetId, int memberId)
    {
        return newKey(_R, targetId, memberId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TARGET_ID, MEMBER_ID); }
    // AUTO-GENERATED: METHODS END
}
