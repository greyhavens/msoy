//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Toy. */
@Entity
@Table
public class ToyRatingRecord extends RatingRecord<ToyRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ToyRatingRecord}
     * with the supplied key values.
     */
    public static Key<ToyRatingRecord> getKey (int itemId, int memberId)
    {
        return new Key<ToyRatingRecord>(
                ToyRatingRecord.class,
                new String[] { ITEM_ID, MEMBER_ID },
                new Comparable[] { itemId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
