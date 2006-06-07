//
// $Id$

package com.threerings.msoy.server;

import com.threerings.util.Name;

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
    @Override protected void populateBootstrapData (BootstrapData data)
    {
        super.populateBootstrapData(data);

        ((MsoyBootstrapData) data).chatOid = MsoyServer.chatOid;
    }

    @Override protected void assignStartingUsername ()
    {
        Name credName = _creds.getUsername();
        if (credName.toString().startsWith("guest")) {
            _username = getNextGuestName();

        } else {
            _username = credName;
        }
    }

    // TEMP: assign sequential guest ids
    protected static Name getNextGuestName ()
    {
        String val = String.valueOf(++_guestCount);
        while (val.length() < 3) {
            val = "0" + val;
        }
        return new Name("guest" + val);
    }
    protected static int _guestCount;
    // END: Temp
}
