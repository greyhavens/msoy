//
// $Id$

package com.threerings.msoy.data;

import com.threerings.util.ActionScript;
import com.threerings.util.Name;

import com.threerings.presents.net.Credentials;

/**
 * Contains extra information used during authentication with the game server.
 */
public class MsoyCredentials extends Credentials
{
    /** A string prepended to session tokens that represent guest sessions. */
    public static final String GUEST_SESSION_PREFIX = "G";

    /** A session token that identifies a user without requiring username or password. */
    public String sessionToken;

    /**
     * Returns true if the supplied (non-null) session token is a guest session token.
     */
    public static boolean isGuestSessionToken (String sessionToken)
    {
        return sessionToken.startsWith(GUEST_SESSION_PREFIX);
    }

    /**
     * Encodes a member id into a guest session token.
     */
    public static String makeGuestSessionToken (int memberId)
    {
        return GUEST_SESSION_PREFIX + memberId;
    }

    /**
     * Returns the member id encoded in a guest session token.
     */
    public static int getGuestMemberId (String sessionToken)
    {
        // if the token is invalid, we let our caller handle the number format exception
        return Integer.parseInt(sessionToken.substring(GUEST_SESSION_PREFIX.length()));
    }

    /**
     * Creates credentials with the specified username. {@link #sessionToken} should be set before
     * logging in.
     */
    public MsoyCredentials (Name username)
    {
        super(username);
    }

    /**
     * Creates a blank instance for unserialization.
     */
    public MsoyCredentials ()
    {
    }

    @Override @ActionScript(name="toStringBuf")
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", token=").append(sessionToken);
    }
}
