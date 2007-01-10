//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;

/**
 * Defines swiftly-related services available to the GWT/AJAX web client.
 */
public interface SwiftlyService extends RemoteService
{
    /**
     * Returns the URL to the Swiftly XML-RPC server.
     */
    public String getRpcURL ()
        throws ServiceException;
}
