//
// $Id$

package com.threerings.msoy.mail.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Links a member to a conversation.
 */
@Entity
public class ParticipantRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ParticipantRecord> _R = ParticipantRecord.class;
    public static final ColumnExp CONVERSATION_ID = colexp(_R, "conversationId");
    public static final ColumnExp PARTICIPANT_ID = colexp(_R, "participantId");
    public static final ColumnExp LAST_READ = colexp(_R, "lastRead");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The conversation in question. */
    @Id public int conversationId;

    /** The id of the member that is participating. */
    @Id @Index(name="ixParticipant")
    public int participantId;

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
                new ColumnExp[] { CONVERSATION_ID, PARTICIPANT_ID },
                new Comparable[] { conversationId, participantId });
    }
    // AUTO-GENERATED: METHODS END
}
