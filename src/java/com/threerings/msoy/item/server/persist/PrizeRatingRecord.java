//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/** Rating records for Prizes. */
public class PrizeRatingRecord extends RatingRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(PrizeRatingRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(PrizeRatingRecord.class, MEMBER_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(PrizeRatingRecord.class, RATING);
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PrizeRatingRecord}
     * with the supplied key values.
     */
    public static Key<PrizeRatingRecord> getKey (int itemId, int memberId)
    {
        return new Key<PrizeRatingRecord>(
                PrizeRatingRecord.class,
                new String[] { ITEM_ID, MEMBER_ID },
                new Comparable[] { itemId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
