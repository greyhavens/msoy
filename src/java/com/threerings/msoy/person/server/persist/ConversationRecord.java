//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.person.data.Conversation;

/**
 * Contains information on a conversation between two members.
 */
@Entity(indices={
    @Index(name="ixLastSent", fields={ ConversationRecord.LAST_SENT })
})
public class ConversationRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #conversationId} field. */
    public static final String CONVERSATION_ID = "conversationId";

    /** The qualified column identifier for the {@link #conversationId} field. */
    public static final ColumnExp CONVERSATION_ID_C =
        new ColumnExp(ConversationRecord.class, CONVERSATION_ID);

    /** The column identifier for the {@link #subject} field. */
    public static final String SUBJECT = "subject";

    /** The qualified column identifier for the {@link #subject} field. */
    public static final ColumnExp SUBJECT_C =
        new ColumnExp(ConversationRecord.class, SUBJECT);

    /** The column identifier for the {@link #lastSent} field. */
    public static final String LAST_SENT = "lastSent";

    /** The qualified column identifier for the {@link #lastSent} field. */
    public static final ColumnExp LAST_SENT_C =
        new ColumnExp(ConversationRecord.class, LAST_SENT);

    /** The column identifier for the {@link #lastAuthorId} field. */
    public static final String LAST_AUTHOR_ID = "lastAuthorId";

    /** The qualified column identifier for the {@link #lastAuthorId} field. */
    public static final ColumnExp LAST_AUTHOR_ID_C =
        new ColumnExp(ConversationRecord.class, LAST_AUTHOR_ID);

    /** The column identifier for the {@link #lastSnippet} field. */
    public static final String LAST_SNIPPET = "lastSnippet";

    /** The qualified column identifier for the {@link #lastSnippet} field. */
    public static final ColumnExp LAST_SNIPPET_C =
        new ColumnExp(ConversationRecord.class, LAST_SNIPPET);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** A unique identifier for this conversation. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int conversationId;

    /** The subject of this conversation. */
    public String subject;

    /** The time at which the most recent message was sent in this conversation. */
    public Timestamp lastSent;

    /** The id of the author of the most recent message. */
    public int lastAuthorId;

    /** A snippet of the text of the most recent message. */
    public String lastSnippet;

    /**
     * Converts this persistent record to a runtime record. The {@link Conversation#lastAuthor} is
     * not filled in.
     */
    public Conversation toConversation ()
    {
        Conversation convo = new Conversation();
        convo.conversationId = conversationId;
        convo.subject = subject;
        convo.lastSent = new Date(lastSent.getTime());
        convo.lastSnippet = lastSnippet;
        return convo;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ConversationRecord}
     * with the supplied key values.
     */
    public static Key<ConversationRecord> getKey (int conversationId)
    {
        return new Key<ConversationRecord>(
                ConversationRecord.class,
                new String[] { CONVERSATION_ID },
                new Comparable[] { conversationId });
    }
    // AUTO-GENERATED: METHODS END
}
