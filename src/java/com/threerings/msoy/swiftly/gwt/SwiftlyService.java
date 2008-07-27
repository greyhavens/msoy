//
// $Id$

package com.threerings.msoy.swiftly.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines Swiftly-related services available to the GWT/AJAX web client.
 */
public interface SwiftlyService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/swiftlysvc";

    /**
     * Returns the SwiftlyConnectConfig used by the applet for connecting to the server.
     */
    public SwiftlyConnectConfig getConnectConfig (WebIdent ident, int projectId)
        throws ServiceException;

    /**
     * Returns the list of SwiftlyProjects that are remixable.
     */
    public List<SwiftlyProject> getRemixableProjects (WebIdent ident)
        throws ServiceException;

    /**
     * Returns the list of SwiftlyProjects on which the supplied member is a collaborator.
     */
    public List<SwiftlyProject> getMembersProjects (WebIdent ident)
        throws ServiceException;

    /**
     * Creates a new SwiftlyProject for the member in the supplied WebIdent.
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
     * Deletes a SwiftlyProject
     */
    public void deleteProject (WebIdent ident, int projectId)
        throws ServiceException;

    /**
     * Loads the SwiftlyProject using the supplied projectId.
     */
    public SwiftlyProject loadProject (WebIdent ident, int projectId)
        throws ServiceException;

    /**
     * Loads the MemberName of the project owner.
     */
    public MemberName getProjectOwner (WebIdent ident, int projectId)
        throws ServiceException;

    /**
     * Loads the collaborators for the given project.
     */
    public List<MemberName> getProjectCollaborators (WebIdent ident, int projectId)
        throws ServiceException;

    /**
     * Loads the friends for a given member.
     */
    public List<FriendEntry> getFriends (WebIdent ident)
        throws ServiceException;

    /**
     * Removes a collaborator from a project
     */
    public void leaveCollaborators (WebIdent ident, int projectId, MemberName name)
        throws ServiceException;

    /**
     * Adds a collaborator from a project.
     * @return the MemberName record of the member that just joined.
     */
    public void joinCollaborators (WebIdent ident, int projectId, MemberName name)
        throws ServiceException;
}
