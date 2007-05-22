//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines swiftly-related services available to the GWT/AJAX web client.
 */
public interface SwiftlyService extends RemoteService
{
    /**
     * Returns the list of SwiftlyProjects that are remixable
     */
    public List getRemixableProjects (WebIdent ident)
        throws ServiceException;

    /**
     * Returns the list of SwiftlyProjects that the supplied member is a collaborator on
     */
    public List getMembersProjects (WebIdent ident)
        throws ServiceException;

    /**
     * Creates a new SwiftlyProject for the member in the supplied WebIdent
     */
    public SwiftlyProject createProject (WebIdent ident, String projectName, byte projectType,
                                         boolean remixable)
        throws ServiceException;

    /**
     * Updates a project in the database if any changes have occurred.
     */
    public void updateProject (WebIdent ident, SwiftlyProject project)
        throws ServiceException;

    /**
     * Loads the SwiftlyProject using the supplied projectId
     */
    public SwiftlyProject loadProject (WebIdent ident, int projectId)
        throws ServiceException;

    /**
     * Loads the configuration needed to load the Swiftly applet.
     */
    public ConnectConfig loadConnectConfig (WebIdent ident)
        throws ServiceException;

    /**
     * Loads the collaborators for the given project.
     */
    public List getProjectCollaborators (WebIdent ident, int projectId)
        throws ServiceException;

    /**
     * Loads the friends for a given member.
     */
    public List getFriends (WebIdent ident)
        throws ServiceException;

    /**
     * Removes a collaborator from a project
     */
    public void leaveCollaborators (WebIdent ident, int projectId, int memberId)
        throws ServiceException;

    /**
     * Adds a collaborator from a project
     */
    public void joinCollaborators (WebIdent ident, int projectId, int memberId)
        throws ServiceException;
}
