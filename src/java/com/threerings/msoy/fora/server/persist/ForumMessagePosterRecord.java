//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Computed record for loading metadata of a forum message.
 */
@Computed(shadowOf=ForumMessageRecord.class)
@Entity
public class ForumMessagePosterRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ForumMessagePosterRecord> _R = ForumMessagePosterRecord.class;
    public static final ColumnExp MESSAGE_ID = colexp(_R, "messageId");
    public static final ColumnExp THREAD_ID = colexp(_R, "threadId");
    public static final ColumnExp POSTER_ID = colexp(_R, "posterId");
    public static final ColumnExp CREATED = colexp(_R, "created");
    // AUTO-GENERATED: FIELDS END

    /** This message's unique identifier. */
    @Id
    public int messageId;

    /** The id of the thread to which this message belongs. */
    public int threadId;

    /** The id of the member that posted this message. */
    public int posterId;

    /** The time at which this message was created. */
    public Timestamp created;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ForumMessagePosterRecord}
     * with the supplied key values.
     */
    public static Key<ForumMessagePosterRecord> getKey (int messageId)
    {
        return new Key<ForumMessagePosterRecord>(
                ForumMessagePosterRecord.class,
                new ColumnExp[] { MESSAGE_ID },
                new Comparable[] { messageId });
    }
    // AUTO-GENERATED: METHODS END
}
