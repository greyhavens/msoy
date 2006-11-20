//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations

import java.sql.Date;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.MemberName;

/**
 * Contains persistent data stored for every member of MetaSOY.
 */
@Entity
public class MemberRecord
    implements Cloneable
{
    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    public static final String MEMBER_ID = "memberId";
    public static final String ACCOUNT_NAME = "accountName";
    public static final String NAME = "name";
    public static final String FLOW = "flow";
    public static final String HOME_SCENE_ID = "homeSceneId";
    public static final String AVATAR_ID = "avatarId";
    public static final String SESSIONS = "sessions";
    public static final String SESSION_MINUTES = "sessionMinutes";
    public static final String LAST_SESSION = "lastSession";

    /** This member's unique id. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int memberId;

    /** The authentication account associated with this member. */
    @Column(unique=true)
    public String accountName;

    /** The name by which this member is known in MetaSOY. */
    public String name;

    /** The quantity of flow possessed by this member. */
    public int flow;

    /** The home scene for this member. */
    public int homeSceneId;

    /** The avatar of this user, or 0. */
    public int avatarId;

    /** The time at which this player was created (when they first starting
     * playing  this particular game). */
    public Date created;

    /** The number of sessions this player has played. */
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    public Date lastSession;

    /** Various one bit data. */
    public int flags;

    /** A blank constructor used when loading records from the database. */
    public MemberRecord ()
    {
    }

    /** Constructs a blank member record for the supplied account. */
    public MemberRecord (String accountName)
    {
        this.accountName = accountName;
    }

    /** Returns true if the specified flag is set. */
    public boolean isSet (int flag)
    {
        return (flags & flag) == flag;
    }

    /** Returns this member's name as a proper {@link Name} instance. */
    public MemberName getName ()
    {
        return new MemberName(name, memberId);
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
