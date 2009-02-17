//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;

/**
 * Contains information used to keep track of who we are in our GWT web application.
 */
public class WebCreds implements IsSerializable
{
    /** This user's role. Each successive role has more privileges than the last. */
    public static enum Role { PERMAGUEST, REGISTERED, VALIDATED, SUPPORT, ADMIN, MAINTAINER };

    /** Our session token. */
    public String token;

    /** The name used to authenticate this account. */
    public String accountName;

    /** Our member name and id. */
    public MemberName name;

    /** The permaname assigned to this account. */
    public String permaName;

    /** This member's role. */
    public Role role;

    /** Returns the name of the creds cookie for use by this deployment. */
    public static String credsCookie ()
    {
        return DeploymentConfig.devDeployment ? "devcreds" : "creds";
    }

    /**
     * Creates and initializes an instance from supplied {@link #flatten}ed string.
     */
    public static WebCreds unflatten (Iterator<String> data)
    {
        if (data == null) {
            return null;
        }
        return new WebCreds(
            data.next(), data.next(), new MemberName(data.next(), Integer.valueOf(data.next())),
            data.next(), Enum.valueOf(Role.class, data.next()));
    }

    /**
     * Creates a configured web creds instance.
     */
    public WebCreds (String token, String accountName, MemberName name, String permaName, Role role)
    {
        this.token = token;
        this.accountName = accountName;
        this.name = name;
        this.permaName = permaName;
        this.role = role;
    }

    /**
     * For GWT, do not use!
     */
    public WebCreds ()
    {
        this(null, null, null, null, null);
    }

    /**
     * Returns the member id for the user authenticated with these credentials.
     */
    public int getMemberId ()
    {
        return (name == null) ? 0 : name.getMemberId();
    }

    /**
     * Returns true if this member is an unregistered (but persistent) guest account.
     */
    public boolean isPermaguest ()
    {
        return role == Role.PERMAGUEST || isRegistered();
    }

    /**
     * Returns true if this member is registered but not validated.
     */
    public boolean isRegistered ()
    {
        return role == Role.REGISTERED || isValidated();
    }

    /**
     * Returns true if this member is a validated user.
     */
    public boolean isValidated ()
    {
        return role == Role.VALIDATED || isSupport();
    }

    /**
     * Returns true if this member has the support role (or higher).
     */
    public boolean isSupport ()
    {
        return role == Role.SUPPORT || isAdmin();
    }

    /**
     * Returns true if this member has the admin role (or higher).
     */
    public boolean isAdmin ()
    {
        return role == Role.ADMIN || isMaintainer();
    }

    /**
     * Returns true if this member has the maintainer role.
     */
    public boolean isMaintainer ()
    {
        return role == Role.MAINTAINER;
    }

    /**
     * Flattens this instance into a string that can be passed between JavaScript apps.
     */
    public List<String> flatten ()
    {
        List<String> data = new ArrayList<String>();
        data.add(token);
        data.add(accountName);
        data.add(name.toString());
        data.add(String.valueOf(name.getMemberId()));
        data.add(permaName);
        data.add(role.toString());
        return data;
    }

    /**
     * Generates a string representaton of this instance.
     */
    public String toString ()
    {
        return "[auth=" + accountName + ", name=" + name + ", pname=" + permaName +
            ", token=" + token + ", role=" + role + "]";
    }
}
