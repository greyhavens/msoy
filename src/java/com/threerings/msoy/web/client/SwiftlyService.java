//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.SwiftlyConfig;
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
     * Returns the list of project types for the member in the supplied WebCreds
     */
    public ArrayList getProjectTypes (WebCreds creds)
        throws ServiceException;

    /**
     * Creates a new SwiftlyProject for the member in the supplied WebCreds
     */
    public SwiftlyProject createProject (WebCreds creds, String projectName, int projectType)
        throws ServiceException;

    /**
     * Loads the SwiftlyProject using the supplied projectId
     */
    public SwiftlyProject loadProject (WebCreds creds, int projectId)
        throws ServiceException;

    /**
     * Loads the configuration needed to load the Swiftly applet.
     */
    public SwiftlyConfig loadSwiftlyConfig (WebCreds creds)
        throws ServiceException;
}
