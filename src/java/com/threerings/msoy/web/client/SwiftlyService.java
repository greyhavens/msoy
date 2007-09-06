//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.SwiftlyConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines Swiftly-related services available to the GWT/AJAX web client.
 */
public interface SwiftlyService extends RemoteService
{
    /**
     * Returns the SwiftlyConnectConfig used by the applet for connecting to the server.
     */
    public SwiftlyConnectConfig getConnectConfig (WebIdent ident, int projectId)
        throws ServiceException;

    /**
     * Returns the list of SwiftlyProjects that are remixable.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.SwiftlyProject>
     */
    public List getRemixableProjects (WebIdent ident)
        throws ServiceException;

    /**
     * Returns the list of SwiftlyProjects on which the supplied member is a collaborator.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.SwiftlyProject>
     */
    public List getMembersProjects (WebIdent ident)
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
     *
     * @gwt.typeArgs <com.threerings.msoy.data.all.MemberName>
     */
    public List getProjectCollaborators (WebIdent ident, int projectId)
        throws ServiceException;

    /**
     * Loads the friends for a given member.
     *
     * @gwt.typeArgs <com.threerings.msoy.data.all.FriendEntry>
     */
    public List getFriends (WebIdent ident)
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
