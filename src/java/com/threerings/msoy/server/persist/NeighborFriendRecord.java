//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.util.StringUtil;

/**
 * Contains derived data from {@link FriendRecord} and {@link MemberRecord}.
 */
@Computed(shadowOf=MemberRecord.class)
@Entity
public class NeighborFriendRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<NeighborFriendRecord> _R = NeighborFriendRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp FLOW = colexp(_R, "flow");
    public static final ColumnExp HOME_SCENE_ID = colexp(_R, "homeSceneId");
    public static final ColumnExp CREATED = colexp(_R, "created");
    public static final ColumnExp SESSIONS = colexp(_R, "sessions");
    public static final ColumnExp SESSION_MINUTES = colexp(_R, "sessionMinutes");
    public static final ColumnExp LAST_SESSION = colexp(_R, "lastSession");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** This member's unique id. */
    @Id
    public int memberId;

    /** The name by which this member is known in MetaSOY. */
    public String name;

    /** The quantity of flow possessed by this member. */
    public int flow;

    /** The home scene for this member. */
    public int homeSceneId;

    /** The time at which this player was created. */
    public Date created;

    /** The number of sessions this player has played. */
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    public Timestamp lastSession;

    /** Generates a string representation of this instance. */
    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link NeighborFriendRecord}
     * with the supplied key values.
     */
    public static Key<NeighborFriendRecord> getKey (int memberId)
    {
        return new Key<NeighborFriendRecord>(
                NeighborFriendRecord.class,
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
