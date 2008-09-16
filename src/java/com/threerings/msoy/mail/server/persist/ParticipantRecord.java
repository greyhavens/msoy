//
// $Id$

package com.threerings.msoy.mail.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Links a member to a conversation.
 */
@Entity
public class ParticipantRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #conversationId} field. */
    public static final String CONVERSATION_ID = "conversationId";

    /** The qualified column identifier for the {@link #conversationId} field. */
    public static final ColumnExp CONVERSATION_ID_C =
        new ColumnExp(ParticipantRecord.class, CONVERSATION_ID);

    /** The column identifier for the {@link #participantId} field. */
    public static final String PARTICIPANT_ID = "participantId";

    /** The qualified column identifier for the {@link #participantId} field. */
    public static final ColumnExp PARTICIPANT_ID_C =
        new ColumnExp(ParticipantRecord.class, PARTICIPANT_ID);

    /** The column identifier for the {@link #lastRead} field. */
    public static final String LAST_READ = "lastRead";

    /** The qualified column identifier for the {@link #lastRead} field. */
    public static final ColumnExp LAST_READ_C =
        new ColumnExp(ParticipantRecord.class, LAST_READ);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The conversation in question. */
    @Id public int conversationId;

    /** The id of the member that is participating. */
    @Id public int participantId;

    /** The timestamp of this participant's most recently read message in this conversation. */
    public Timestamp lastRead;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ParticipantRecord}
     * with the supplied key values.
     */
    public static Key<ParticipantRecord> getKey (int conversationId, int participantId)
    {
        return new Key<ParticipantRecord>(
                ParticipantRecord.class,
                new String[] { CONVERSATION_ID, PARTICIPANT_ID },
                new Comparable[] { conversationId, participantId });
    }
    // AUTO-GENERATED: METHODS END
}
