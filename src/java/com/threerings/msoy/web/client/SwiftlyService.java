//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Defines swiftly-related services available to the GWT/AJAX web client.
 */
public interface SwiftlyService extends RemoteService
{
    /**
     * Returns the list of SwiftlyProjects for the member in the supplied WebCreds
     */
    public ArrayList getProjects (WebCreds creds)
        throws ServiceException;

    /**
     * Creates a new SwiftlyProject for the member in the supplied WebCreds
     */
    public SwiftlyProject createProject (WebCreds creds, SwiftlyProject project)
        throws ServiceException;
}
