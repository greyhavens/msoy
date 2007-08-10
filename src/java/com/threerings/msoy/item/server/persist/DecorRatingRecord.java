//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

/** Rating records for Decor. */
public class DecorRatingRecord extends RatingRecord<DecorRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #DecorRatingRecord}
     * with the supplied key values.
     */
    public static Key<DecorRatingRecord> getKey (int itemId, int memberId)
    {
        return new Key<DecorRatingRecord>(
                DecorRatingRecord.class,
                new String[] { ITEM_ID, MEMBER_ID },
                new Comparable[] { itemId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
