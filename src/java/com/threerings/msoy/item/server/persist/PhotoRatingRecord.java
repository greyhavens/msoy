//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

/** Rating records for Photos. */
public class PhotoRatingRecord extends RatingRecord<PhotoRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #PhotoRatingRecord}
     * with the supplied key values.
     */
    public static Key<PhotoRatingRecord> getKey (int itemId, int memberId)
    {
        return new Key<PhotoRatingRecord>(
                PhotoRatingRecord.class,
                new String[] { ITEM_ID, MEMBER_ID },
                new Comparable[] { itemId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
