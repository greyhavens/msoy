//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.presents.server.PresentsSession;

import com.threerings.msoy.bureau.data.BureauLauncherClientObject;

/**
 * Represents a bureau launcher connection.
 */
public class BureauLauncherSession extends PresentsSession
{
    @Override
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        // Stuff the name of the host into the client object
        BureauLauncherClientObject clobj = (BureauLauncherClientObject)getClientObject();
        clobj.hostname = getInetAddress().getHostName();
    }
}
