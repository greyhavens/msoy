//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains a mapping from visitorId to the entry vector string supplied when they first created
 * their account. This is a write only table that is never read by Whirled but exists to make
 * correlating entry vector to billing transactions easier down the line.
 */
public class EntryVectorRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<EntryVectorRecord> _R = EntryVectorRecord.class;
    public static final ColumnExp VISITOR_ID = colexp(_R, "visitorId");
    public static final ColumnExp VECTOR = colexp(_R, "vector");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp CREATED = colexp(_R, "created");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The tracking id assigned to the member when they first arrived. */
    @Id public String visitorId;

    /** The entry vector provided by the member if one was provided, null otherwise. */
    public String vector;

    /** The member that this visitor eventually turned into, or 0 if never. This column is indexed
     * because we periodically delete all 0 memberId rows older than a week. */
    @Index public int memberId;

    /** The time at which this record was created. */
    public Timestamp created;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link EntryVectorRecord}
     * with the supplied key values.
     */
    public static Key<EntryVectorRecord> getKey (String visitorId)
    {
        return new Key<EntryVectorRecord>(
                EntryVectorRecord.class,
                new ColumnExp[] { VISITOR_ID },
                new Comparable[] { visitorId });
    }
    // AUTO-GENERATED: METHODS END
}
