//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations

import java.lang.String;

import java.sql.Date;

/**
 * Contains a mapping from a session token to a member id. Used to persist an
 * authenticated session for some period of time.
 */
@Entity
public class SessionRecord
{
    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    public static final String TOKEN = "token";
    public static final String MEMBER_ID = "memberId";
    public static final String EXPIRES = "expires";

    /** The unique session identifier. */
    @Id
    public String token;

    /** The id of the member bound to this session. */
    @Column(unique=true)
    public int memberId;

    /** The time at which this session will be expired. */
    public Date expires;

    /** Used when unserializing. */
    public SessionRecord ()
    {
    }

    /** Used to create a "key" for doing database fiddling. */
    public SessionRecord (String sessionToken)
    {
        token = sessionToken;
    }
}
