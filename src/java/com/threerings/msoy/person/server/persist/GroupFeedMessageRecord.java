//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.GroupFeedMessage;

/**
 * Contains persistent data on a feed message distributed to a group's members.
 */
@Entity
public class GroupFeedMessageRecord extends FeedMessageRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GroupFeedMessageRecord> _R = GroupFeedMessageRecord.class;
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp MESSAGE_ID = colexp(_R, "messageId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp DATA = colexp(_R, "data");
    public static final ColumnExp POSTED = colexp(_R, "posted");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The group id of the originator of this message. */
    @Index(name="ixGroupId")
    public int groupId;

    @Override // from FeedMessageRecord
    protected FeedMessage createMessage ()
    {
        return new GroupFeedMessage();
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GroupFeedMessageRecord}
     * with the supplied key values.
     */
    public static Key<GroupFeedMessageRecord> getKey (int messageId)
    {
        return new Key<GroupFeedMessageRecord>(
                GroupFeedMessageRecord.class,
                new ColumnExp[] { MESSAGE_ID },
                new Comparable[] { messageId });
    }
    // AUTO-GENERATED: METHODS END
}
