//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.SelfFeedMessage;

/**
 * Contains persistent data on a feed message distributed to a member's friends.
 */
@Entity(indices={
    @Index(name="ixTargetId", fields={ SelfFeedMessageRecord.TARGET_ID })
})
public class SelfFeedMessageRecord extends FeedMessageRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #targetId} field. */
    public static final String TARGET_ID = "targetId";

    /** The qualified column identifier for the {@link #targetId} field. */
    public static final ColumnExp TARGET_ID_C =
        new ColumnExp(SelfFeedMessageRecord.class, TARGET_ID);

    /** The column identifier for the {@link #actorId} field. */
    public static final String ACTOR_ID = "actorId";

    /** The qualified column identifier for the {@link #actorId} field. */
    public static final ColumnExp ACTOR_ID_C =
        new ColumnExp(SelfFeedMessageRecord.class, ACTOR_ID);

    /** The qualified column identifier for the {@link #messageId} field. */
    public static final ColumnExp MESSAGE_ID_C =
        new ColumnExp(SelfFeedMessageRecord.class, MESSAGE_ID);

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(SelfFeedMessageRecord.class, TYPE);

    /** The qualified column identifier for the {@link #data} field. */
    public static final ColumnExp DATA_C =
        new ColumnExp(SelfFeedMessageRecord.class, DATA);

    /** The qualified column identifier for the {@link #posted} field. */
    public static final ColumnExp POSTED_C =
        new ColumnExp(SelfFeedMessageRecord.class, POSTED);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The member id of the target for this message. */
    public int targetId;

    /** The member id of the originator of this message (if any). */
    public int actorId;

    @Override // from FeedMessageRecord
    protected FeedMessage createMessage ()
    {
        return new SelfFeedMessage();
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SelfFeedMessageRecord}
     * with the supplied key values.
     */
    public static Key<SelfFeedMessageRecord> getKey (int messageId)
    {
        return new Key<SelfFeedMessageRecord>(
                SelfFeedMessageRecord.class,
                new String[] { MESSAGE_ID },
                new Comparable[] { messageId });
    }
    // AUTO-GENERATED: METHODS END
}
