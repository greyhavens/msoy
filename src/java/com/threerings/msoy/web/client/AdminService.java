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

    /**
     * Creates accounts for the supplied email addresses and sends invitation emails to same.
     * Returns a string for each address denoting success or failure.
     */
    public String[] registerAndInvite (WebCreds creds, String[] emails)
        throws ServiceException;
}
