//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

/** Rating records for Avatars. */
public class AvatarRatingRecord extends RatingRecord<AvatarRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #AvatarRatingRecord}
     * with the supplied key values.
     */
    public static Key<AvatarRatingRecord> getKey (int itemId, int memberId)
    {
        return new Key<AvatarRatingRecord>(
                AvatarRatingRecord.class,
                new String[] { ITEM_ID, MEMBER_ID },
                new Comparable[] { itemId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
