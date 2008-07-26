//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;

import com.samskivert.util.IntMap;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.web.data.MemberCard;

import com.threerings.msoy.fora.gwt.Comment;

/**
 * Contains a comment made on an entity of some sort (item, profile, room, etc.).
 */
@Entity(indices={
    @Index(name="ixMemberId", fields={ CommentRecord.MEMBER_ID })
})
public class CommentRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #entityType} field. */
    public static final String ENTITY_TYPE = "entityType";

    /** The qualified column identifier for the {@link #entityType} field. */
    public static final ColumnExp ENTITY_TYPE_C =
        new ColumnExp(CommentRecord.class, ENTITY_TYPE);

    /** The column identifier for the {@link #entityId} field. */
    public static final String ENTITY_ID = "entityId";

    /** The qualified column identifier for the {@link #entityId} field. */
    public static final ColumnExp ENTITY_ID_C =
        new ColumnExp(CommentRecord.class, ENTITY_ID);

    /** The column identifier for the {@link #posted} field. */
    public static final String POSTED = "posted";

    /** The qualified column identifier for the {@link #posted} field. */
    public static final ColumnExp POSTED_C =
        new ColumnExp(CommentRecord.class, POSTED);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(CommentRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #text} field. */
    public static final String TEXT = "text";

    /** The qualified column identifier for the {@link #text} field. */
    public static final ColumnExp TEXT_C =
        new ColumnExp(CommentRecord.class, TEXT);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The type of entity on which this comment was made (see {@link Comment}). */
    @Id
    public int entityType;

    /** The entity-specific identifier for the entity on which this comment was made. */
    @Id
    public int entityId;

    /** The time at which this comment was posted. */
    @Id
    public Timestamp posted;

    /** The id of the member that made this comment. */
    public int memberId;

    /** The text of this comment. */
    @Column(length=Comment.MAX_TEXT_LENGTH)
    public String text;

    /**
     * Converts this persistent record to a runtime record.
     *
     * @param names a mapping from member id to {@link MemberCard} that should contain {@link
     * #memberId}.
     */
    public Comment toComment (IntMap<MemberCard> cards)
    {
        Comment comment = new Comment();
        MemberCard card = cards.get(memberId);
        if (card != null) {
            comment.commentor = card.name;
            comment.photo = card.photo;
        }
        comment.posted = posted.getTime();
        comment.text = text;
        return comment;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #CommentRecord}
     * with the supplied key values.
     */
    public static Key<CommentRecord> getKey (int entityType, int entityId, Timestamp posted)
    {
        return new Key<CommentRecord>(
                CommentRecord.class,
                new String[] { ENTITY_TYPE, ENTITY_ID, POSTED },
                new Comparable[] { entityType, entityId, posted });
    }
    // AUTO-GENERATED: METHODS END
}
