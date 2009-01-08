//
// $Id: CommentRecord.java 13300 2008-11-16 08:05:06Z mdb $

package com.threerings.msoy.comment.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.comment.gwt.Comment;

/**
 * Tracks a member's rating of a comment.
 */
@Entity
public class CommentRatingRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CommentRatingRecord> _R = CommentRatingRecord.class;
    public static final ColumnExp ENTITY_TYPE = colexp(_R, "entityType");
    public static final ColumnExp ENTITY_ID = colexp(_R, "entityId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp POSTED = colexp(_R, "posted");
    public static final ColumnExp RATING = colexp(_R, "rating");
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
    @Id @Index
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
                new ColumnExp[] { ENTITY_TYPE, ENTITY_ID, MEMBER_ID, POSTED },
                new Comparable[] { entityType, entityId, memberId, posted });
    }
    // AUTO-GENERATED: METHODS END
}
