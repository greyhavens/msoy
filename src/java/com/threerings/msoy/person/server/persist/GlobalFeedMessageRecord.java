//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.person.gwt.FeedMessage;

/**
 * Contains persistent data for a global feed message.
 */
@Entity
public class GlobalFeedMessageRecord extends FeedMessageRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GlobalFeedMessageRecord> _R = GlobalFeedMessageRecord.class;
    public static final ColumnExp MESSAGE_ID = colexp(_R, "messageId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp DATA = colexp(_R, "data");
    public static final ColumnExp POSTED = colexp(_R, "posted");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

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
                new ColumnExp[] { MESSAGE_ID },
                new Comparable[] { messageId });
    }
    // AUTO-GENERATED: METHODS END
}
