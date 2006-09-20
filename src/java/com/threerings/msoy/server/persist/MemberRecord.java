//
// $Id$

package com.threerings.msoy.server.persist;

import javax.persistence.*; // for EJB3 annotations

import java.sql.Date;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.MemberName;

/**
 * Contains persistent data stored for every member of MetaSOY.
 */
@Entity
public class MemberRecord
{
    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    public static final String MEMBER_ID = "memberId";
    public static final String ACCOUNT_NAME = "accountName";
    public static final String NAME = "name";
    public static final String FLOW = "flow";
    public static final String HOME_SCENE_ID = "homeSceneId";
    public static final String SESSIONS = "sessions";
    public static final String SESSION_MINUTES = "sessionMinutes";
    public static final String LAST_SESSION = "lastSession";

    /** This member's unique id. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int memberId;

    /** The authentication account associated with this member. */
    @Column(unique=true, nullable=false)
    public String accountName;

    /** The name by which this member is known in MetaSOY. */
    public String name;

    /** The quantity of flow possessed by this member. */
    @Column(nullable=false)
    public int flow;

    /** The home scene for this member. */
    @Column(nullable=false)
    public int homeSceneId;

    /** The time at which this player was created (when they first starting
     * playing  this particular game). */
    @Column(nullable=false)
    public Date created;

    /** The number of sessions this player has played. */
    @Column(nullable=false)
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    @Column(nullable=false)
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    @Column(nullable=false)
    public Date lastSession;

    /** Various one bit data. */
    @Column(nullable=false)
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
