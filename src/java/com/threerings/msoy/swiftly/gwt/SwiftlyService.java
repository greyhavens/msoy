//
// $Id$

package com.threerings.msoy.swiftly.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.msoy.web.data.ServiceException;

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
    SwiftlyConnectConfig getConnectConfig (int projectId)
        throws ServiceException;

    /**
     * Returns the list of SwiftlyProjects that are remixable.
     */
    List<SwiftlyProject> getRemixableProjects ()
        throws ServiceException;

    /**
     * Returns the list of SwiftlyProjects on which the calling member is a collaborator.
     */
    List<SwiftlyProject> getMembersProjects ()
        throws ServiceException;

    /**
     * Creates a new SwiftlyProject for the calling member.
     */
    SwiftlyProject createProject (String projectName, byte projectType, boolean remixable)
        throws ServiceException;

    /**
     * Updates a project in the database if any changes have occurred.
     */
    void updateProject (SwiftlyProject project)
        throws ServiceException;

    /**
     * Deletes a SwiftlyProject
     */
    void deleteProject (int projectId)
        throws ServiceException;

    /**
     * Loads the SwiftlyProject using the supplied projectId.
     */
    SwiftlyProject loadProject (int projectId)
        throws ServiceException;

    /**
     * Loads the MemberName of the project owner.
     */
    MemberName getProjectOwner (int projectId)
        throws ServiceException;

    /**
     * Loads the collaborators for the given project.
     */
    List<MemberName> getProjectCollaborators (int projectId)
        throws ServiceException;

    /**
     * Loads the friends for the calling member.
     */
    List<FriendEntry> getFriends ()
        throws ServiceException;

    /**
     * Removes a collaborator from a project
     */
    void leaveCollaborators (int projectId, MemberName name)
        throws ServiceException;

    /**
     * Adds a collaborator from a project.
     * @return the MemberName record of the member that just joined.
     */
    void joinCollaborators (int projectId, MemberName name)
        throws ServiceException;
}
