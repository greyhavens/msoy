//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.servlet.user.Password;

import com.threerings.util.ActionScript;
import com.threerings.util.Name;

import com.threerings.msoy.data.MsoyCredentials;

/**
 * Contains information used during authentication of a world session.
 */
public class WorldCredentials extends MsoyCredentials
{
    /** The machine identifier of the client, if one is known. */
    public String ident;

    /** Indicates whether this client is set up as a featured place view. */
    public boolean featuredPlaceView;

    /** The unique tracking id for this client, if one is assigned */
    public String visitorId;

    /**
     * Creates credentials with the specified username and password. {@link #ident} should be set
     * before logging in unless the client does not know its machine identifier in which case it
     * should be left null.
     */
    public WorldCredentials (Name username, Password password)
    {
        super(username);
        _password = password.getEncrypted();
    }

    /**
     * Creates a blank instance for unserialization.
     */
    public WorldCredentials ()
    {
    }

    /**
     * Returns our encrypted password data, or null if none was provided.
     */
    public String getPassword ()
    {
        return _password;
    }

    @Override @ActionScript(name="toStringBuf")
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", password=").append(_password);
        buf.append(", ident=").append(ident);
        buf.append(", featuredPlaceView=").append(featuredPlaceView);
        buf.append(", visitorId=").append(visitorId);
    }

    protected String _password;
}
