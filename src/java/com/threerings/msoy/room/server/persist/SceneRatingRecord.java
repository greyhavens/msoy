//
// $Id$

package com.threerings.msoy.room.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.server.persist.RatingRecord;

/** Rating records for rooms. */
public class SceneRatingRecord extends RatingRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The qualified column identifier for the {@link #targetId} field. */
    public static final ColumnExp TARGET_ID_C =
        new ColumnExp(SceneRatingRecord.class, TARGET_ID);

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(SceneRatingRecord.class, MEMBER_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(SceneRatingRecord.class, RATING);
    // AUTO-GENERATED: FIELDS END


    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SceneRatingRecord}
     * with the supplied key values.
     */
    public static Key<SceneRatingRecord> getKey (int targetId, int memberId)
    {
        return new Key<SceneRatingRecord>(
                SceneRatingRecord.class,
                new String[] { TARGET_ID, MEMBER_ID },
                new Comparable[] { targetId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
