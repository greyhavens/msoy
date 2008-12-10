//
// $Id: CommentRecord.java 13300 2008-11-16 08:05:06Z mdb $

package com.threerings.msoy.comment.server.persist;

import java.sql.Timestamp;
import java.util.Map;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.web.gwt.MemberCard;

import com.threerings.msoy.comment.gwt.Comment;

/**
 * Tracks a member's rating of a comment.
 */
@Entity(indices={
    @Index(name="ixMemberId", fields={ CommentRecord.MEMBER_ID })
})
public class CommentRatingRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #entityType} field. */
    public static final String ENTITY_TYPE = "entityType";

    /** The qualified column identifier for the {@link #entityType} field. */
    public static final ColumnExp ENTITY_TYPE_C =
        new ColumnExp(CommentRatingRecord.class, ENTITY_TYPE);

    /** The column identifier for the {@link #entityId} field. */
    public static final String ENTITY_ID = "entityId";

    /** The qualified column identifier for the {@link #entityId} field. */
    public static final ColumnExp ENTITY_ID_C =
        new ColumnExp(CommentRatingRecord.class, ENTITY_ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(CommentRatingRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #posted} field. */
    public static final String POSTED = "posted";

    /** The qualified column identifier for the {@link #posted} field. */
    public static final ColumnExp POSTED_C =
        new ColumnExp(CommentRatingRecord.class, POSTED);

    /** The column identifier for the {@link #rating} field. */
    public static final String RATING = "rating";

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(CommentRatingRecord.class, RATING);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The type of entity on which the rated comment was made (see {@link Comment}). */
    @Id
    public int entityType;

    /** The entity-specific identifier for the entity on which the rated comment was made. */
    @Id
    public int entityId;

    /** The id of the member that rated the comment. */
    @Id
    public int memberId;

    /** The time at which the rated comment was posted. */
    @Id
    public Timestamp posted;

    /** The actual rating: true for positive, false for negative. */
    public boolean rating;

    public CommentRatingRecord ()
    {
    }

    public CommentRatingRecord (int entityType, int entityId, Timestamp posted,
                                int memberId, boolean rating)
    {
        this.entityType = entityType;
        this.entityId = entityId;
        this.memberId = memberId;
        this.posted = posted;
        this.rating = rating;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CommentRatingRecord}
     * with the supplied key values.
     */
    public static Key<CommentRatingRecord> getKey (int entityType, int entityId, int memberId, Timestamp posted)
    {
        return new Key<CommentRatingRecord>(
                CommentRatingRecord.class,
                new String[] { ENTITY_TYPE, ENTITY_ID, MEMBER_ID, POSTED },
                new Comparable[] { entityType, entityId, memberId, posted });
    }
    // AUTO-GENERATED: METHODS END
}
