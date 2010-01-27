//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;

import com.threerings.io.Streamable;

/**
 * Represents a member's rating of an item.
 */
@Entity
public abstract class RatingRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RatingRecord> _R = RatingRecord.class;
    public static final ColumnExp TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp RATING = colexp(_R, "rating");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The id of the tagged item. */
    @Id
    public int targetId;

    /** The id of the rating member. */
    @Id
    public int memberId;

    /** The rating, from 1 to 5 */
    public byte rating;
}
