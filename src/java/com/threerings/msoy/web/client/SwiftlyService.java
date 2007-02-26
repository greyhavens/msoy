//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

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
     * Returns the list of SwiftlyProjects that are remixable
     */
    public List getRemixableProjects (WebCreds creds)
        throws ServiceException;

    /**
     * Returns the list of SwiftlyProjects that the supplied member is a collaborator on
     */
    public List getMembersProjects (WebCreds creds)
        throws ServiceException;

    /**
     * Returns the list of project types for the member in the supplied WebCreds
     */
    public List getProjectTypes (WebCreds creds)
        throws ServiceException;

    /**
     * Creates a new SwiftlyProject for the member in the supplied WebCreds
     */
    public SwiftlyProject createProject (WebCreds creds, String projectName, int projectType,
                                         boolean remixable)
        throws ServiceException;

    /**
     * Updates a project in the database if any changes have occurred.
     */
    public void updateProject (WebCreds creds, SwiftlyProject project)
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
