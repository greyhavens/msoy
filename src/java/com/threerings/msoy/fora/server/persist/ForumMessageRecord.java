//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Contains information on a single post to a thread.
 */
@Entity(fullTextIndices={
    @FullTextIndex(name=ForumMessageRecord.FTS_MESSAGE, fields={ "message" })
})
public class ForumMessageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ForumMessageRecord> _R = ForumMessageRecord.class;
    public static final ColumnExp<Integer> MESSAGE_ID = colexp(_R, "messageId");
    public static final ColumnExp<Integer> THREAD_ID = colexp(_R, "threadId");
    public static final ColumnExp<Integer> IN_REPLY_TO = colexp(_R, "inReplyTo");
    public static final ColumnExp<Integer> POSTER_ID = colexp(_R, "posterId");
    public static final ColumnExp<Integer> ISSUE_ID = colexp(_R, "issueId");
    public static final ColumnExp<Timestamp> CREATED = colexp(_R, "created");
    public static final ColumnExp<Timestamp> LAST_EDITED = colexp(_R, "lastEdited");
    public static final ColumnExp<String> MESSAGE = colexp(_R, "message");
    // AUTO-GENERATED: FIELDS END

    /** The identifier for the full text search index on {@link #message} */
    public static final String FTS_MESSAGE = "MESSAGE";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 6;

    /** This message's unique identifier. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int messageId;

    /** The id of the thread to which this message belongs. */
    @Index(name="ixThreadId")
    public int threadId;

    /** The id of the message to which this message is a reply, or zero. */
    public int inReplyTo;

    /** The id of the member that posted this message. */
    public int posterId;

    /** The id of the issue to which this message is asoociated, or zero. */
    @Index(name="ixIssueId")
    public int issueId;

    /** The time at which this message was created. */
    @Index(name="ixCreated")
    public Timestamp created;

    /** The time at which this message was last edited. */
    public Timestamp lastEdited;

    /** The text of this message. */
    @Column(length=ForumMessage.MAX_MESSAGE_LENGTH)
    public String message;

    /**
     * Extract the message id from the key.
     */
    public static int extractMessageId (Key<ForumMessageRecord> key)
    {
        return (Integer)(key.getValues()[0]);
    }

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
     * Create and return a primary {@link Key} to identify a {@link ForumMessageRecord}
     * with the supplied key values.
     */
    public static Key<ForumMessageRecord> getKey (int messageId)
    {
        return newKey(_R, messageId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MESSAGE_ID); }
    // AUTO-GENERATED: METHODS END
}
