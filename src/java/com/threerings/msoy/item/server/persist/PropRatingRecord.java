//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.server.persist.RatingRecord;

/** Rating records for Props. */
public class PropRatingRecord extends RatingRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PropRatingRecord> _R = PropRatingRecord.class;
    public static final ColumnExp TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp RATING = colexp(_R, "rating");
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PropRatingRecord}
     * with the supplied key values.
     */
    public static Key<PropRatingRecord> getKey (int targetId, int memberId)
    {
        return new Key<PropRatingRecord>(
                PropRatingRecord.class,
                new ColumnExp[] { TARGET_ID, MEMBER_ID },
                new Comparable[] { targetId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
