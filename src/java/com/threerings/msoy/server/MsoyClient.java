//
// $Id$

package com.threerings.msoy.server;

import com.threerings.presents.net.BootstrapData;

import com.threerings.whirled.server.WhirledClient;

import com.threerings.msoy.data.MsoyBootstrapData;

/**
 * Represents an attached Msoy client on the server-side.
 */
public class MsoyClient extends WhirledClient
{
    // documentation inherited
    protected BootstrapData createBootstrapData ()
    {
        return new MsoyBootstrapData();
    }

    // documentation inherited
    protected void populateBootstrapData (BootstrapData data)
    {
        super.populateBootstrapData(data);

        ((MsoyBootstrapData) data).chatOid = MsoyServer.chatOid;
    }
}
