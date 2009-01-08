//
// $Id$

package com.threerings.msoy.mail.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.mail.gwt.Conversation;

/**
 * Contains information on a conversation between two members.
 */
@Entity
public class ConversationRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ConversationRecord> _R = ConversationRecord.class;
    public static final ColumnExp CONVERSATION_ID = colexp(_R, "conversationId");
    public static final ColumnExp INITIATOR_ID = colexp(_R, "initiatorId");
    public static final ColumnExp TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp SUBJECT = colexp(_R, "subject");
    public static final ColumnExp LAST_SENT = colexp(_R, "lastSent");
    public static final ColumnExp LAST_AUTHOR_ID = colexp(_R, "lastAuthorId");
    public static final ColumnExp LAST_SNIPPET = colexp(_R, "lastSnippet");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** A unique identifier for this conversation. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int conversationId;

    /** The member id of the initiator of this conversation. */
    public int initiatorId;

    /** The member id of the target of this conversation. */
    public int targetId;

    /** The subject of this conversation. */
    public String subject;

    /** The time at which the most recent message was sent in this conversation. */
    @Index(name="ixLastSent")
    public Timestamp lastSent;

    /** The id of the author of the most recent message. */
    public int lastAuthorId;

    /** A snippet of the text of the most recent message. */
    public String lastSnippet;

    /**
     * Returns the member id of the other party to this conversation.
     */
    public int getOtherId (int readerId)
    {
        return (readerId == initiatorId) ? targetId : initiatorId;
    }

    /**
     * Converts this persistent record to a runtime record. {@link Conversation#other} and {@link
     * Conversation#hasUnread} are not filled in.
     */
    public Conversation toConversation ()
    {
        Conversation convo = new Conversation();
        convo.conversationId = conversationId;
        convo.subject = subject;
        convo.lastSent = new Date(lastSent.getTime());
        convo.lastAuthorId = lastAuthorId;
        convo.lastSnippet = lastSnippet;
        return convo;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ConversationRecord}
     * with the supplied key values.
     */
    public static Key<ConversationRecord> getKey (int conversationId)
    {
        return new Key<ConversationRecord>(
                ConversationRecord.class,
                new ColumnExp[] { CONVERSATION_ID },
                new Comparable[] { conversationId });
    }
    // AUTO-GENERATED: METHODS END
}
