//
// $Id$

package com.threerings.msoy.web.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
     * Unflattens a string created by a call to {@link #flatten}.
     */
    public static WebCreds unflatten (Iterator<String> data)
    {
        if (data == null) {
            return null;
        }

        WebCreds creds = new WebCreds();
        creds.token = data.next();
        creds.accountName = data.next();
        creds.name = new MemberName(data.next(), Integer.valueOf(data.next()));
        creds.permaName = data.next();
        creds.isSupport = Boolean.valueOf(data.next());
        creds.isAdmin = Boolean.valueOf(data.next());
        return creds;
    }

    /**
     * Flattens these creds into a string that can be passed between JavaScript apps.
     */
    public List<String> flatten ()
    {
        List<String> data = new ArrayList<String>();
        data.add(token);
        data.add(accountName);
        data.add(name.toString());
        data.add(String.valueOf(name.getMemberId()));
        data.add(permaName);
        data.add(String.valueOf(isSupport));
        data.add(String.valueOf(isAdmin));
        return data;
    }

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
