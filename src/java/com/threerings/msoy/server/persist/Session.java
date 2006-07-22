//
// $Id$

package com.threerings.msoy.server.persist;

import java.lang.String;

import java.sql.Date;

/**
 * Contains a mapping from a session token to a member id. Used to persist an
 * authenticated session for some period of time.
 */
public class Session
{
    /** The id of the member bound to this session. */
    public int memberId;

    /** The unique session identifier. */
    public String token;

    /** The time at which this session will be expired. */
    public Date expires;

    /** Used when unserializing. */
    public Session ()
    {
    }

    /** Used to create a "key" for doing database fiddling. */
    public Session (String sessionToken)
    {
        token = sessionToken;
    }
}
