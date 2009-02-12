//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.Name;

import com.threerings.msoy.data.MsoyCredentials;

/**
 * Used to authenticate with an MSOY Game server.
 */
public class GameCredentials extends MsoyCredentials
{
    /** The unique tracking id for this client, if one is assigned */
    public String visitorId;

    @Override // from MsoyCredentials
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", visitorId=").append(visitorId);
    }
}
