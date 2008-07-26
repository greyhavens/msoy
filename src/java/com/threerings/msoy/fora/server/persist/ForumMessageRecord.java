//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.FullTextIndex;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.web.data.MemberCard;

import com.threerings.msoy.fora.gwt.ForumMessage;

/**
 * Contains information on a single post to a thread.
 */
@Entity(indices={
    @Index(name="ixCreated", fields={ ForumMessageRecord.CREATED }),
    @Index(name="ixIssueId", fields={ ForumMessageRecord.ISSUE_ID })
}, fullTextIndices={
    @FullTextIndex(name=ForumMessageRecord.FTS_MESSAGE, fields={ ForumMessageRecord.MESSAGE })
})
public class ForumMessageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #messageId} field. */
    public static final String MESSAGE_ID = "messageId";

    /** The qualified column identifier for the {@link #messageId} field. */
    public static final ColumnExp MESSAGE_ID_C =
        new ColumnExp(ForumMessageRecord.class, MESSAGE_ID);

    /** The column identifier for the {@link #threadId} field. */
    public static final String THREAD_ID = "threadId";

    /** The qualified column identifier for the {@link #threadId} field. */
    public static final ColumnExp THREAD_ID_C =
        new ColumnExp(ForumMessageRecord.class, THREAD_ID);

    /** The column identifier for the {@link #inReplyTo} field. */
    public static final String IN_REPLY_TO = "inReplyTo";

    /** The qualified column identifier for the {@link #inReplyTo} field. */
    public static final ColumnExp IN_REPLY_TO_C =
        new ColumnExp(ForumMessageRecord.class, IN_REPLY_TO);

    /** The column identifier for the {@link #posterId} field. */
    public static final String POSTER_ID = "posterId";

    /** The qualified column identifier for the {@link #posterId} field. */
    public static final ColumnExp POSTER_ID_C =
        new ColumnExp(ForumMessageRecord.class, POSTER_ID);

    /** The column identifier for the {@link #issueId} field. */
    public static final String ISSUE_ID = "issueId";

    /** The qualified column identifier for the {@link #issueId} field. */
    public static final ColumnExp ISSUE_ID_C =
        new ColumnExp(ForumMessageRecord.class, ISSUE_ID);

    /** The column identifier for the {@link #created} field. */
    public static final String CREATED = "created";

    /** The qualified column identifier for the {@link #created} field. */
    public static final ColumnExp CREATED_C =
        new ColumnExp(ForumMessageRecord.class, CREATED);

    /** The column identifier for the {@link #lastEdited} field. */
    public static final String LAST_EDITED = "lastEdited";

    /** The qualified column identifier for the {@link #lastEdited} field. */
    public static final ColumnExp LAST_EDITED_C =
        new ColumnExp(ForumMessageRecord.class, LAST_EDITED);

    /** The column identifier for the {@link #message} field. */
    public static final String MESSAGE = "message";

    /** The qualified column identifier for the {@link #message} field. */
    public static final ColumnExp MESSAGE_C =
        new ColumnExp(ForumMessageRecord.class, MESSAGE);
    // AUTO-GENERATED: FIELDS END

    /** The identifier for the full text search index on {@link #message} */
    public static final String FTS_MESSAGE = "MESSAGE";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 5;

    /** This message's unique identifier. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int messageId;

    /** The id of the thread to which this message belongs. */
    public int threadId;

    /** The id of the message to which this message is a reply, or zero. */
    public int inReplyTo;

    /** The id of the member that posted this message. */
    public int posterId;

    /** The id of the issue to which this message is asoociated, or zero. */
    public int issueId;

    /** The time at which this message was created. */
    public Timestamp created;

    /** The time at which this message was last edited. */
    public Timestamp lastEdited;

    /** The text of this message. */
    @Column(length=ForumMessage.MAX_MESSAGE_LENGTH)
    public String message;

    /**
     * Converts this persistent record into a runtime record.
     *
     * @param members a mapping from memberId to {@link MemberCard} that should contain a mapping
     * for {@link #posterId}.
     */
    public ForumMessage toForumMessage (Map<Integer,MemberCard> members)
    {
        ForumMessage msg = new ForumMessage();
        msg.messageId = messageId;
        msg.threadId = threadId;
        msg.inReplyTo = inReplyTo;
        msg.poster = members.get(posterId);
        msg.issueId = issueId;
        msg.created = new Date(created.getTime());
        msg.lastEdited = new Date(lastEdited.getTime());
        msg.message = message;
        return msg;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ForumMessageRecord}
     * with the supplied key values.
     */
    public static Key<ForumMessageRecord> getKey (int messageId)
    {
        return new Key<ForumMessageRecord>(
                ForumMessageRecord.class,
                new String[] { MESSAGE_ID },
                new Comparable[] { messageId });
    }
    // AUTO-GENERATED: METHODS END
}
