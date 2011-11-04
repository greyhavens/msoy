//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;

/**
 * Contains information used to keep track of who we are in our GWT web application.
 */
public class WebCreds
    implements IsSerializable
{
    /** This user's role. Each successive role has more privileges than the last. */
    public static enum Role
        implements IsSerializable
    {
        PERMAGUEST, REGISTERED, SUBSCRIBER, SUPPORT, ADMIN, MAINTAINER
    };

    /** Our session token. */
    public String token;

    /** The name used to authenticate this account. */
    public String accountName;

    /** Is this user's email address validated? */
    public boolean validated;

    /** Is this member a recent registrant? */
    public boolean isNewbie;

    /** Our member name and id. */
    public MemberName name;

    /** The permaname assigned to this account. */
    public String permaName;

    /** This member's role. */
    public Role role;

    /** Whether to start load the Flash client. */
    public boolean autoFlash;

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
            data.next(), data.next(), Boolean.valueOf(data.next()), Boolean.valueOf(data.next()),
            new MemberName(data.next(), Integer.valueOf(data.next())), data.next(),
            Role.valueOf(data.next()), Boolean.valueOf(data.next()));
    }

    /**
     * Creates a configured web creds instance.
     */
    public WebCreds (String token, String accountName, boolean validated, boolean isNewbie,
                     MemberName name, String permaName, Role role, boolean autoFlash)
    {
        this.token = token;
        this.accountName = accountName;
        this.validated = validated;
        this.isNewbie = isNewbie;
        this.name = name;
        this.permaName = permaName;
        this.role = role;
        this.autoFlash = autoFlash;
    }

    /**
     * For GWT, do not use!
     */
    public WebCreds ()
    {
        this(null, null, false, false, null, null, null, false);
    }

    /**
     * Returns the member id for the user authenticated with these credentials.
     */
    public int getMemberId ()
    {
        return (name == null) ? 0 : name.getId();
    }

    /**
     * Returns true if this user has an account (is not an anonymous guest).
     */
    public boolean isMember ()
    {
        return roleAtLeast(Role.PERMAGUEST);
    }

    /**
     * Returns true if this user is a registered member (or better).
     */
    public boolean isRegistered ()
    {
        return roleAtLeast(Role.REGISTERED);
    }

    /**
     * Returns true if this member is a subscriber (or better).
     */
    public boolean isSubscriber ()
    {
        return roleAtLeast(Role.SUBSCRIBER);
    }

    /**
     * Returns true if this member has the support role (or higher).
     */
    public boolean isSupport ()
    {
        return roleAtLeast(Role.SUPPORT);
    }

    /**
     * Returns true if this member has the admin role (or higher).
     */
    public boolean isAdmin ()
    {
        return roleAtLeast(Role.ADMIN);
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
        List<String> data = Lists.newArrayList();
        data.add(token);
        data.add(accountName);
        data.add(String.valueOf(validated));
        data.add(String.valueOf(isNewbie));
        data.add(name.toString());
        data.add(String.valueOf(name.getId()));
        data.add(permaName);
        data.add(String.valueOf(role));
        data.add(String.valueOf(autoFlash));
        return data;
    }

    /**
     * Generates a string representaton of this instance.
     */
    public String toString ()
    {
        return "[auth=" + accountName + ", validated=" + validated +
            ", name=" + name + ", pname=" + permaName +
            ", token=" + token + ", role=" + role + "]";
    }

    /**
     * Return true if this role has at least the privileges of the specified role.
     */
    protected boolean roleAtLeast (Role required)
    {
        return (role != null) && (role.ordinal() >= required.ordinal());
    }
}
