//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.person.gwt.FeedMessage;

/**
 * Contains persistent data for a global feed message.
 */
@Entity
public class GlobalFeedMessageRecord extends FeedMessageRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The qualified column identifier for the {@link #messageId} field. */
    public static final ColumnExp MESSAGE_ID_C =
        new ColumnExp(GlobalFeedMessageRecord.class, MESSAGE_ID);

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(GlobalFeedMessageRecord.class, TYPE);

    /** The qualified column identifier for the {@link #data} field. */
    public static final ColumnExp DATA_C =
        new ColumnExp(GlobalFeedMessageRecord.class, DATA);

    /** The qualified column identifier for the {@link #posted} field. */
    public static final ColumnExp POSTED_C =
        new ColumnExp(GlobalFeedMessageRecord.class, POSTED);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    @Override // from FeedMessageRecord
    protected FeedMessage createMessage ()
    {
        return new FeedMessage();
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GlobalFeedMessageRecord}
     * with the supplied key values.
     */
    public static Key<GlobalFeedMessageRecord> getKey (int messageId)
    {
        return new Key<GlobalFeedMessageRecord>(
                GlobalFeedMessageRecord.class,
                new String[] { MESSAGE_ID },
                new Comparable[] { messageId });
    }
    // AUTO-GENERATED: METHODS END
}
