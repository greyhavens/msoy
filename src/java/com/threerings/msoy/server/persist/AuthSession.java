//
// $Id$

package com.threerings.msoy.server.persist;

import javax.persistence.*; // for EJB3 annotations
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.lang.String;

import java.sql.Date;

/**
 * Contains a mapping from a session token to a member id. Used to persist an
 * authenticated session for some period of time.
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class AuthSession
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
    public AuthSession ()
    {
    }

    /** Used to create a "key" for doing database fiddling. */
    public AuthSession (String sessionToken)
    {
        token = sessionToken;
    }
}
