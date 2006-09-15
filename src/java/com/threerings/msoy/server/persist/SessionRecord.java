//
// $Id$

package com.threerings.msoy.server.persist;

import javax.persistence.*; // for EJB3 annotations

import java.lang.String;

import java.sql.Date;

/**
 * Contains a mapping from a session token to a member id. Used to persist an
 * authenticated session for some period of time.
 */
@Entity
public class SessionRecord
{
    /** The unique session identifier. */
    @Id
    public String token;

    /** The id of the member bound to this session. */
    @Column(unique=true, nullable=false)
    public int memberId;

    /** The time at which this session will be expired. */
    @Column(nullable=false)
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
