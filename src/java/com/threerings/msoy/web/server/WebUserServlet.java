//
// $Id$

package com.threerings.msoy.web.server;

import com.threerings.msoy.client.LogonException;
import com.threerings.msoy.client.WebUserService;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.MsoyServer;

/**
 * Provides the server implementation of {@link WebUserService}.
 */
public class WebUserServlet
    implements WebUserService
{
    // from interface WebUserService
    public String login (String username, String password, boolean persist)
        throws LogonException
    {
        // we are running on a servlet thread at this point and can thus talk
        // to the authenticator directly as it is thread safe (and it blocks)
        // and we are allowed to block
        MsoyAuthenticator auth = (MsoyAuthenticator)
            MsoyServer.conmgr.getAuthenticator();
        return auth.authenticateSession(username, password, persist);
    }
}
