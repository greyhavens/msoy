//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.servlet.user.Password;

import com.threerings.presents.net.UsernamePasswordCreds;
import com.threerings.util.Name;

/**
 * Contains extra information used during authentication with the game server.
 */
public class MsoyCredentials extends UsernamePasswordCreds
{
    /** The machine identifier of the client, if one is known. */
    public String ident;

    /**
     * Creates credentials with the specified username and password.
     * {@link #ident} should be set before logging in unless the client does
     * not know its machine identifier in which case it should be left null.
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
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", ident=").append(ident);
    }
}
