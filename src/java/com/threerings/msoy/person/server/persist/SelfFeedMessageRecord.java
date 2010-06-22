//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.util.Map;
import java.util.Set;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.SelfFeedMessage;

/**
 * Contains persistent data on a feed message distributed to a member's friends.
 */
@Entity
public class SelfFeedMessageRecord extends FeedMessageRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SelfFeedMessageRecord> _R = SelfFeedMessageRecord.class;
    public static final ColumnExp TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp ACTOR_ID = colexp(_R, "actorId");
    public static final ColumnExp MESSAGE_ID = colexp(_R, "messageId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp DATA = colexp(_R, "data");
    public static final ColumnExp POSTED = colexp(_R, "posted");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** The member id of the target for this message. */
    @Index(name="ixTargetId")
    public int targetId;

    /** The member id of the originator of this message (if any). */
    public int actorId;

    @Override // from FeedMessageRecord
    public void addReferences (Set<Integer> memberIds, Set<Integer> groupIds)
    {
        // NOTE: the target id is not used when creating the runtime message so is not needed here
        memberIds.add(actorId);
    }

    @Override // from FeedMessageRecord
    public FeedMessage toMessage (
        Map<Integer, MemberName> memberNames, Map<Integer, GroupName> groupNames)
    {
        return new SelfFeedMessage(getType(), memberNames.get(actorId), getData(), getPosted());
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SelfFeedMessageRecord}
     * with the supplied key values.
     */
    public static Key<SelfFeedMessageRecord> getKey (int messageId)
    {
        return newKey(_R, messageId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MESSAGE_ID); }
    // AUTO-GENERATED: METHODS END
}
