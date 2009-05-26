//
// $Id$

package com.threerings.msoy.data;

import com.threerings.util.ActionScript;

import com.threerings.presents.net.Credentials;

/**
 * Contains extra information used during authentication with the game server.
 */
public class MsoyCredentials extends Credentials
{
    /** A session token that identifies a user without requiring username or password. */
    public String sessionToken;

    /** The unique tracking id for this client, if one is assigned */
    public String visitorId;

    /** The affiliate id provided to the client via Flash parameters or 0. */
    public int affiliateId;

    /** The vector by which this client entered, or null. */
    public String vector;

    @Override @ActionScript(name="toStringBuf")
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", token=").append(sessionToken);
        buf.append(", visitorId=").append(visitorId);
        buf.append(", affiliateId=").append(affiliateId);
        buf.append(", vector=").append(vector);
    }
}
