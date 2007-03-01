//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Defines remote services available to admins.
 */
public interface AdminService extends RemoteService
{
    /**
     * Loads the configuration needed to run the Dashboard applet.
     */
    public ConnectConfig loadConnectConfig (WebCreds creds)
        throws ServiceException;
}
