//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

/** Rating records for Games. */
public class GameRatingRecord extends RatingRecord<GameRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GameRatingRecord}
     * with the supplied key values.
     */
    public static Key<GameRatingRecord> getKey (int itemId, int memberId)
    {
        return new Key<GameRatingRecord>(
                GameRatingRecord.class,
                new String[] { ITEM_ID, MEMBER_ID },
                new Comparable[] { itemId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
