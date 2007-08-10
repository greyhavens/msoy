//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

/** Rating records for Videos. */
public class VideoRatingRecord extends RatingRecord<VideoRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #VideoRatingRecord}
     * with the supplied key values.
     */
    public static Key<VideoRatingRecord> getKey (int itemId, int memberId)
    {
        return new Key<VideoRatingRecord>(
                VideoRatingRecord.class,
                new String[] { ITEM_ID, MEMBER_ID },
                new Comparable[] { itemId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
