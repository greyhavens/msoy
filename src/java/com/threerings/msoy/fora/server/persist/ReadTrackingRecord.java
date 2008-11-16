//
// $Id$

package com.threerings.msoy.fora.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains read tracking information for a particular member and a particular thread.
 */
@Entity
public class ReadTrackingRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(ReadTrackingRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #threadId} field. */
    public static final String THREAD_ID = "threadId";

    /** The qualified column identifier for the {@link #threadId} field. */
    public static final ColumnExp THREAD_ID_C =
        new ColumnExp(ReadTrackingRecord.class, THREAD_ID);

    /** The column identifier for the {@link #lastReadPostId} field. */
    public static final String LAST_READ_POST_ID = "lastReadPostId";

    /** The qualified column identifier for the {@link #lastReadPostId} field. */
    public static final ColumnExp LAST_READ_POST_ID_C =
        new ColumnExp(ReadTrackingRecord.class, LAST_READ_POST_ID);

    /** The column identifier for the {@link #lastReadPostIndex} field. */
    public static final String LAST_READ_POST_INDEX = "lastReadPostIndex";

    /** The qualified column identifier for the {@link #lastReadPostIndex} field. */
    public static final ColumnExp LAST_READ_POST_INDEX_C =
        new ColumnExp(ReadTrackingRecord.class, LAST_READ_POST_INDEX);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The id of the member in question. */
    @Id public int memberId;

    /** The id of the thread in question. */
    @Id public int threadId;

    /** The most recent post id of the thread the last time the membe read it. */
    public int lastReadPostId;

    /** The ordinal index of the post in its containing thread. Because all threads are tracked
     * with global identifiers this is needed. */
    public int lastReadPostIndex;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ReadTrackingRecord}
     * with the supplied key values.
     */
    public static Key<ReadTrackingRecord> getKey (int memberId, int threadId)
    {
        return new Key<ReadTrackingRecord>(
                ReadTrackingRecord.class,
                new String[] { MEMBER_ID, THREAD_ID },
                new Comparable[] { memberId, threadId });
    }
    // AUTO-GENERATED: METHODS END
}
