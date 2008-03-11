//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.servlet.user.Password;

import com.samskivert.util.StringUtil;
import com.threerings.util.ActionScript;
import com.threerings.util.Name;

import com.threerings.presents.net.UsernamePasswordCreds;

/**
 * Contains extra information used during authentication with the game server.
 */
public class MsoyCredentials extends UsernamePasswordCreds
{
    /** A string prepended to session tokens that represent guest sessions. */
    public static final String GUEST_SESSION_PREFIX = "G_";

    /** A session token that identifies a user without requiring username or password. */
    public String sessionToken;

    /** The machine identifier of the client, if one is known. */
    public String ident;

    /** Indicates whether this client is set up as a featured place view. */
    public boolean featuredPlaceView;

    /**
     * Converts a session token supplied by a guest into its underlying bytes.
     */
    public static byte[] getGuestTokenData (String sessionToken)
    {
        return StringUtil.unhexlate(sessionToken.substring(GUEST_SESSION_PREFIX.length()));
    }

    /**
     * Creates credentials with the specified username and password.  {@link #ident} should be set
     * before logging in unless the client does not know its machine identifier in which case it
     * should be left null.
     */
    public MsoyCredentials (Name username, Password password)
    {
        super(username, password.getEncrypted());
    }

    /**
     * Creates a blank instance for unserialization.
     */
    public MsoyCredentials ()
    {
    }

    @Override // documentation inherited
    @ActionScript(name="toStringBuf")
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", ident=").append(ident);
        buf.append(", featuredPlaceView=").append(featuredPlaceView);
    }
}
