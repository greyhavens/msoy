//
// $Id$

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

import com.threerings.msoy.comment.data.all.Comment;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Contains a comment made on an entity of some sort (item, profile, room, etc.).
 */
@Entity
public class CommentRecord extends PersistentRecord
    implements Comparable<CommentRecord>
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CommentRecord> _R = CommentRecord.class;
    public static final ColumnExp<Integer> ENTITY_TYPE = colexp(_R, "entityType");
    public static final ColumnExp<Integer> ENTITY_ID = colexp(_R, "entityId");
    public static final ColumnExp<Timestamp> POSTED = colexp(_R, "posted");
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Integer> CURRENT_RATING = colexp(_R, "currentRating");
    public static final ColumnExp<Integer> TOTAL_RATINGS = colexp(_R, "totalRatings");
    public static final ColumnExp<String> TEXT = colexp(_R, "text");
    public static final ColumnExp<Timestamp> REPLY_TO = colexp(_R, "replyTo");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 4;

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
    @Index
    public int memberId;

    /** The absolute rating of this comment. */
    @Column(defaultValue="1")
    public int currentRating;

    /** The total number of times this comment has been rated. */
    public int totalRatings;

    /** The text of this comment. */
    @Column(length=Comment.MAX_TEXT_LENGTH)
    public String text;

    /** The post date of the original comment this comment is replying to. Null if not a reply. */
    @Index @Column(nullable=true)
    public Timestamp replyTo;

    /**
     * Converts this persistent record to a runtime record.
     *
     * @param cards a mapping from member id to {@link MemberCard} that should contain {@link
     * #memberId}.
     */
    public Comment toComment (Map<Integer, MemberCard> cards)
    {
        Comment comment = new Comment();
        MemberCard card = cards.get(memberId);
        if (card != null) {
            comment.commentor = card.name;
            comment.photo = card.photo;
        }
        comment.posted = posted.getTime();
        if (replyTo != null) {
            comment.replyTo = replyTo.getTime();
        }

        comment.currentRating = currentRating;
        comment.entityId = entityId;
        comment.totalRatings = totalRatings;
        comment.text = text;
        return comment;
    }

    public int compareTo (CommentRecord that)
    {
        return this.posted.compareTo(that.posted);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CommentRecord}
     * with the supplied key values.
     */
    public static Key<CommentRecord> getKey (int entityType, int entityId, Timestamp posted)
    {
        return newKey(_R, entityType, entityId, posted);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ENTITY_TYPE, ENTITY_ID, POSTED); }
    // AUTO-GENERATED: METHODS END
}
