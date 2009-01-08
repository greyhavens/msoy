//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.server.persist.RatingRecord;

/** Rating records for Toy. */
public class ToyRatingRecord extends RatingRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ToyRatingRecord> _R = ToyRatingRecord.class;
    public static final ColumnExp TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp RATING = colexp(_R, "rating");
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ToyRatingRecord}
     * with the supplied key values.
     */
    public static Key<ToyRatingRecord> getKey (int targetId, int memberId)
    {
        return new Key<ToyRatingRecord>(
                ToyRatingRecord.class,
                new ColumnExp[] { TARGET_ID, MEMBER_ID },
                new Comparable[] { targetId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
