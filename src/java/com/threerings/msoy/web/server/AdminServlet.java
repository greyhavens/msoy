//
// $Id$

package com.threerings.msoy.web.server;

import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.AdminService;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides the server implementation of {@link AdminService}.
 */
public class AdminServlet extends MsoyServiceServlet
    implements AdminService
{
    // from interface AdminService
    public ConnectConfig loadConnectConfig (WebCreds creds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        ConnectConfig config = new ConnectConfig();
        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        return config;
    }
}
