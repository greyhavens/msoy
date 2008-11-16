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
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(NeighborFriendRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(NeighborFriendRecord.class, NAME);

    /** The column identifier for the {@link #flow} field. */
    public static final String FLOW = "flow";

    /** The qualified column identifier for the {@link #flow} field. */
    public static final ColumnExp FLOW_C =
        new ColumnExp(NeighborFriendRecord.class, FLOW);

    /** The column identifier for the {@link #homeSceneId} field. */
    public static final String HOME_SCENE_ID = "homeSceneId";

    /** The qualified column identifier for the {@link #homeSceneId} field. */
    public static final ColumnExp HOME_SCENE_ID_C =
        new ColumnExp(NeighborFriendRecord.class, HOME_SCENE_ID);

    /** The column identifier for the {@link #created} field. */
    public static final String CREATED = "created";

    /** The qualified column identifier for the {@link #created} field. */
    public static final ColumnExp CREATED_C =
        new ColumnExp(NeighborFriendRecord.class, CREATED);

    /** The column identifier for the {@link #sessions} field. */
    public static final String SESSIONS = "sessions";

    /** The qualified column identifier for the {@link #sessions} field. */
    public static final ColumnExp SESSIONS_C =
        new ColumnExp(NeighborFriendRecord.class, SESSIONS);

    /** The column identifier for the {@link #sessionMinutes} field. */
    public static final String SESSION_MINUTES = "sessionMinutes";

    /** The qualified column identifier for the {@link #sessionMinutes} field. */
    public static final ColumnExp SESSION_MINUTES_C =
        new ColumnExp(NeighborFriendRecord.class, SESSION_MINUTES);

    /** The column identifier for the {@link #lastSession} field. */
    public static final String LAST_SESSION = "lastSession";

    /** The qualified column identifier for the {@link #lastSession} field. */
    public static final ColumnExp LAST_SESSION_C =
        new ColumnExp(NeighborFriendRecord.class, LAST_SESSION);
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
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
