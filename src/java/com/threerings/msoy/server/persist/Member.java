//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.MemberName;

/**
 * Contains persistent data stored for every member of MetaSOY.
 */
public class Member
{
    /** This member's unique id. */
    public int memberId;

    /** The authentication account associated with this member. */
    public String accountName;

    /** The name by which this member is known in MetaSOY. */
    public String name;

    /** The quantity of flow possessed by this member. */
    public int flow;

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
    public Member ()
    {
    }

    /** Constructs a blank member record for the supplied account. */
    public Member (String accountName)
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
