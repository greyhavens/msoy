//
// $Id$

package com.threerings.msoy.data;

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
    }

    protected String _password;
}
