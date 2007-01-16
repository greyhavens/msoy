//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations

import java.sql.Date;

import com.samskivert.util.StringUtil;

/**
 * Contains derived data from {@link FriendRecord} and {@link MemberRecord} tailored
 * to the construction of a {@link NeighborMember} object.
 */
@Computed
@Entity
public class NeighborFriendRecord extends PersistentRecord
{
    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    public static final String MEMBER_ID = "memberId";
    public static final String NAME = "name";
    public static final String FLOW = "flow";
    public static final String CREATED = "created";
    public static final String SESSIONS = "sessions";
    public static final String SESSION_MINUTES = "sessionMinutes";
    public static final String LAST_SESSION = "lastSession";

    /** This member's unique id. */
    @Id
    public int memberId;

    /** The name by which this member is known in MetaSOY. */
    public String name;

    /** The quantity of flow possessed by this member. */
    public int flow;

    /** The time at which this player was created. */
    public Date created;

    /** The number of sessions this player has played. */
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    public Date lastSession;

    /** Generates a string representation of this instance. */
    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
