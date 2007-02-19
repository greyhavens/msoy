//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

import java.sql.Date;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Contains persistent data stored for every member of MetaSOY.
 */
@Entity
public class MemberRecord extends PersistentRecord
{
    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    public static final String MEMBER_ID = "memberId";
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberRecord.class, MEMBER_ID);

    public static final String ACCOUNT_NAME = "accountName";
    public static final ColumnExp ACCOUNT_NAME_C =
        new ColumnExp(MemberRecord.class, ACCOUNT_NAME);

    public static final String NAME = "name";
    public static final ColumnExp NAME_C =
        new ColumnExp(MemberRecord.class, NAME);

    public static final String FLOW = "flow";
    public static final ColumnExp FLOW_C =
        new ColumnExp(MemberRecord.class, FLOW);

    public static final String HOME_SCENE_ID = "homeSceneId";
    public static final ColumnExp HOME_SCENE_ID_C =
        new ColumnExp(MemberRecord.class, HOME_SCENE_ID);

    public static final String AVATAR_ID = "avatarId";
    public static final ColumnExp AVATAR_ID_C =
        new ColumnExp(MemberRecord.class, AVATAR_ID);

    public static final String CREATED = "created";
    public static final ColumnExp CREATED_C =
        new ColumnExp(MemberRecord.class, CREATED);

    public static final String SESSIONS = "sessions";
    public static final ColumnExp SESSIONS_C =
        new ColumnExp(MemberRecord.class, SESSIONS);

    public static final String SESSION_MINUTES = "sessionMinutes";
    public static final ColumnExp SESSION_MINUTES_C =
        new ColumnExp(MemberRecord.class, SESSION_MINUTES);

    public static final String LAST_SESSION = "lastSession";
    public static final ColumnExp LAST_SESSION_C =
        new ColumnExp(MemberRecord.class, LAST_SESSION);

    /** A flag denoting this user as having support privileges. */
    public static final int SUPPORT_FLAG = 0x1 << 0;

    /** A flag denoting this user as having admin privileges. */
    public static final int ADMIN_FLAG = 0x1 << 1;

    /** A flag denoting this user has having elected to see mature content. */
    public static final int FLAG_SHOW_MATURE = 0x1 << 2;

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
     * playing this particular game). */
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

    /**
     * Creates web credentials for this member record.
     */
    public WebCreds toCreds (String authtok)
    {
        WebCreds creds = new WebCreds();
        creds.token = authtok;
        creds.accountName = accountName;
        creds.name = getName();
        creds.isSupport = isSupport();
        creds.isAdmin = isAdmin();
        return creds;
    }

    /**
     * Returns true if this member has support or higher privileges.
     */
    public boolean isSupport ()
    {
        return isSet(SUPPORT_FLAG) || isSet(ADMIN_FLAG);
    }

    /**
     * Returns true if this member has admin or higher privileges.
     */
    public boolean isAdmin ()
    {
        return isSet(ADMIN_FLAG);
    }

    /**
     * Tests whether a given flag is set on this member.
     */
    public boolean isSet (int flag)
    {
        return (flags & flag) != 0;
    }

    /**
     * Sets a given flag to on or off.
     */
    public void setFlag (int flag, boolean value)
    {
        flags = value ? flags | flag : flags ^ ~flag;
    }

    /** Returns this member's name as a proper {@link Name} instance. */
    public MemberName getName ()
    {
        return new MemberName(name, memberId);
    }

    /**
     * Returns a brief string containing our account name, member id and display name.
     */
    public String who ()
    {
        return accountName + " (" + memberId + ", " + name + ")";
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
