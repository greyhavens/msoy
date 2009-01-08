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
    public static final Class<ReadTrackingRecord> _R = ReadTrackingRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp THREAD_ID = colexp(_R, "threadId");
    public static final ColumnExp LAST_READ_POST_ID = colexp(_R, "lastReadPostId");
    public static final ColumnExp LAST_READ_POST_INDEX = colexp(_R, "lastReadPostIndex");
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
                new ColumnExp[] { MEMBER_ID, THREAD_ID },
                new Comparable[] { memberId, threadId });
    }
    // AUTO-GENERATED: METHODS END
}
