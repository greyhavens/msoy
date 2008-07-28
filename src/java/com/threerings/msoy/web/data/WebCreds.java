//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;

/**
 * Contains information used to keep track of who we are in our GWT web application.
 */
public class WebCreds implements IsSerializable
{
    /** The name of the cookie in which we store our session credentials. */
    public static final String CREDS_COOKIE = "creds";

    /** Our session token. */
    public String token;

    /** The name used to authenticate this account. */
    public String accountName;

    /** Our member name and id. */
    public MemberName name;

    /** The permaname assigned to this account. */
    public String permaName;

    /** Indicates that the authenticated user has support (or admin) privileges. */
    public boolean isSupport;

    /** Indicates that the authenticated user has admin privileges. */
    public boolean isAdmin;

    /**
     * Returns the member id for the user authenticated with these credentials.
     */
    public int getMemberId ()
    {
        return (name == null) ? 0 : name.getMemberId();
    }

    /**
     * Generates a string representaton of this instance.
     */
    public String toString ()
    {
        return "[auth=" + accountName + ", name=" + name + ", pname=" + permaName +
            ", token=" + token + ", privs=" + isSupport + ":" + isAdmin + "]";
    }
}
