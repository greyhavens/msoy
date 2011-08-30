//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

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
    public static final ColumnExp<Integer> TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Byte> RATING = colexp(_R, "rating");
    public static final ColumnExp<Timestamp> TIMESTAMP = colexp(_R, "timestamp");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    /** Provides the {@link #memberId} of a record. */
    public static final Function<RatingRecord,Integer> GET_MEMBER_ID =
        new Function<RatingRecord,Integer>() {
        public Integer apply (RatingRecord record) {
            return record.memberId;
        }
    };

    /** The id of the rated item. */
    @Id
    public int targetId;

    /** The id of the rating member. */
    @Id @Index(name="memberIx")
    public int memberId;

    /** The rating, from 1 to 5 */
    public byte rating;

    /** When this member most recently rated this item. */
    @Column(defaultValue="'2010-01-01'")
    public Timestamp timestamp;
}
