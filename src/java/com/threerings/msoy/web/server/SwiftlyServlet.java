//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.web.client.SwiftlyService;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.WebIdent;

import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.server.ProjectRoomManager;
import com.threerings.msoy.swiftly.server.persist.SwiftlyCollaboratorsRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link SwiftlyService}.
 */
public class SwiftlyServlet extends MsoyServiceServlet
    implements SwiftlyService
{
    // from SwiftlyService
    public List getRemixableProjects (WebIdent ident)
        throws ServiceException
    {
        requireAuthedUser(ident);

        ArrayList<SwiftlyProject> projects = new ArrayList<SwiftlyProject>();
        try {
            for (SwiftlyProjectRecord pRec :
                MsoyServer.swiftlyRepo.findRemixableProjects()) {
                projects.add(pRec.toSwiftlyProject());
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting remixable projects failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        return projects;
    }

    // from SwiftlyService
    public List getMembersProjects (WebIdent ident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        ArrayList<SwiftlyProject> projects = new ArrayList<SwiftlyProject>();
        try {
            for (SwiftlyProjectRecord pRec :
                MsoyServer.swiftlyRepo.findMembersProjects(memrec.memberId)) {
                projects.add(pRec.toSwiftlyProject());
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting user's projects failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        return projects;
    }

    // from SwiftlyService
    public SwiftlyProject createProject (WebIdent ident, String projectName, byte projectType,
                                         boolean remixable)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        SwiftlyProject project;
        SwiftlyProjectRecord pRec;
        SwiftlySVNStorageRecord storeRec;

        // TODO Argument Validation
        /*
        if (!isValidName(project.name)) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
        */

        if (!SwiftlyProject.isValidProjectType(projectType)) {
            log.warning("Failed to create project. Invalid project type. [type=" +
                projectType + "]");
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // Initialize the project storage.
        try {
            // XXX We need to sort out how to properly create remote repositories.
            // Until then, we create them in a hard-wired local directory.
            String svnRoot = ServerConfig.serverRoot + "/data/swiftly/projects";
            storeRec = MsoyServer.swiftlyRepo.createSVNStorage(ProjectSVNStorage.PROTOCOL_FILE,
                "", 0, svnRoot);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Creating new project storage record failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // Create the project record.
        try {
            pRec = MsoyServer.swiftlyRepo.createProject(
                memrec.memberId, projectName, projectType, storeRec.storageId, remixable);
            project = pRec.toSwiftlyProject();

            // Set the creator as the first collaborator.
            MsoyServer.swiftlyRepo.joinCollaborators(pRec.projectId, memrec.memberId);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Creating new project failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // If the repository initialization fails, we do our best to roll back any database
        // modifications. Hopefully that works.  Oh, what I'd give for transactions!

        // Initialize the SVN storage
        try {
            // Load the template from the standard path
            File templatePath = new File(ServerConfig.serverRoot + "/data/swiftly/templates/"
                + Item.getTypeName(projectType));
            ProjectSVNStorage.initializeStorage(project, storeRec, templatePath);

        } catch (ProjectStorageException pse) {
            log.log(Level.WARNING, "Initializing swiftly project storage failed.", pse);
            try {
                MsoyServer.swiftlyRepo.deleteProject(pRec);
            } catch (PersistenceException pe) {
                log.log(Level.WARNING,
                        "Deleting the partially-initialized swiftly project failed.", pe);
            }
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        return project;
    }

    // from SwiftlyService
    public void updateProject (WebIdent ident, SwiftlyProject project)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        requireOwner(project.projectId, memrec.memberId);

        // TODO Argument Validation
        /*
        if (!isValidName(project.name)) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
        */

        try {
            SwiftlyProjectRecord pRec = MsoyServer.swiftlyRepo.loadProject(project.projectId);
            if (pRec == null) {
                throw new PersistenceException("Swiftly project not found! [id=" +
                    project.projectId + "]");
            }
            Map<String, Object> updates = pRec.findUpdates(project);
            if (updates.size() > 0) {
                MsoyServer.swiftlyRepo.updateProject(project.projectId, updates);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Updating project failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from SwiftlyService
    public void deleteProject (WebIdent ident, int projectId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        requireOwner(projectId, memrec.memberId);

        try {
            MsoyServer.swiftlyRepo.markProjectDeleted(projectId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Marking project deleted failed [id= " + projectId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from SwiftlyService
    public SwiftlyProject loadProject (WebIdent ident, int projectId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        try {
            SwiftlyProjectRecord pRec = MsoyServer.swiftlyRepo.loadProject(projectId);
            if (pRec == null) {
                throw new ServiceException(SwiftlyCodes.E_NO_SUCH_PROJECT);
            }
            // verify the user has permission to view this project
            if (!pRec.remixable && !isCollaborator(pRec.projectId, memrec.memberId)) {
                throw new ServiceException(SwiftlyCodes.ACCESS_DENIED);
            }
            return pRec.toSwiftlyProject();

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Loading project failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface SwiftlyService
    public ConnectConfig loadConnectConfig (WebIdent ident)
        throws ServiceException
    {
        requireAuthedUser(ident);

        // create an applet config record
        ConnectConfig config = new ConnectConfig();
        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        config.httpPort = ServerConfig.httpPort;
        return config;
    }

    // from SwiftlyService
    public List getProjectCollaborators (WebIdent ident, int projectId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        requireCollaborator(projectId, memrec.memberId);

        ArrayList<MemberName> members = new ArrayList<MemberName>();

        try {
            for (SwiftlyCollaboratorsRecord cRec :
                MsoyServer.swiftlyRepo.getCollaborators(projectId)) {
                members.add(MsoyServer.memberRepo.loadMember(cRec.memberId).getName());
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting project's collaborators failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        return members;
    }

    // from SwiftlyService
    public List<FriendEntry> getFriends (WebIdent ident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        try {
            return MsoyServer.memberRepo.getFriends(memrec.memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting member's friends failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from SwiftlyService
    public void leaveCollaborators (WebIdent ident, int projectId, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        requireOwner(projectId, memrec.memberId);

        // Don't let the owner remove themselves.
        if (isOwner(projectId, memberId)) {
            log.warning("Refusing to remove the project owner from collaborators. [projectId=" +
                projectId + "]");
            return;
        }

        try {
            MsoyServer.swiftlyRepo.leaveCollaborators(projectId, memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Removing project's collaborators failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // inform the project room manager of the change in collaborators
        updateRoomCollaborators(projectId);
    }

    // from SwiftlyService
    public MemberName joinCollaborators (WebIdent ident, int projectId, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        requireOwner(projectId, memrec.memberId);

        // if the user is already a collaborator, do nothing
        if (isCollaborator(projectId, memberId)) {
            log.warning("Refusing to add an existing collaborator to project. [projectId=" +
                projectId + ", memberId=" + memberId + "]");
            return null;
        }

        MemberName member = null;
        try {
            MsoyServer.swiftlyRepo.joinCollaborators(projectId, memberId);
            member = MsoyServer.memberRepo.loadMember(memberId).getName();
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Joining project's collaborators failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // inform the project room manager of the change in collaborators
        updateRoomCollaborators(projectId);

        return member;
    }

    /**
     * Informs the room manager for this project, if resolved, that the collaborators have
     * been modified.
     */
    protected void updateRoomCollaborators (final int projectId)
        throws ServiceException
    {
        // run a task on the dobject thread that first finds the ProjectRoomManager for this
        // project if it exists, and then tells it to update its local list of collaborators
        final ServletWaiter<Void> waiter =
            new ServletWaiter<Void>("updateCollaborators[" + projectId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                ProjectRoomManager manager = MsoyServer.swiftlyMan.getRoomManager(projectId);
                // the room manager is not resolved, no problem, we updated the database
                if (manager == null) {
                    waiter.requestCompleted(null);
                    return;
                }

                manager.updateCollaborators(waiter);
            }
        });

        // block the servlet waiting for the dobject thread
        waiter.waitForResult();
    }

    /**
     * Verifies a member is a collaborator of a project.
     * @param projectId the id of the project being tested
     * @param memberId the id of the member being tested
     * @throws ServiceException thrown if the memberId is not a collaborator.
     */
    protected void requireCollaborator (int projectId, int memberId)
        throws ServiceException
    {
        if (!isCollaborator(projectId, memberId)) {
            throw new ServiceException(SwiftlyCodes.ACCESS_DENIED);
        }
    }

    /**
     * Verifies a member is the owner of a project.
     * @param projectId the id of the project being tested
     * @param memberId the id of the member being tested
     * @throws ServiceException thrown if the memberId is not the owner.
     */
    protected void requireOwner (int projectId, int memberId)
        throws ServiceException
    {
        if (!isOwner(projectId, memberId)) {
            throw new ServiceException(SwiftlyCodes.ACCESS_DENIED);
        }
    }

    /**
     * Determines if a member is a collaborator on the supplied project.
     * @param projectId the id of the project being tested
     * @param memberId the id of the member being tested
     * @return true if memberId is a collaborator, false otherwise.
     * @throws ServiceException thrown if PersistenceException is encountered
     */
    protected boolean isCollaborator (int projectId, int memberId)
        throws ServiceException
    {
        try {
            return MsoyServer.swiftlyRepo.isCollaborator(projectId, memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Checking project membership failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    /**
     * Determines if a member is the owner of the supplied project.
     * @param projectId the id of the project being tested
     * @param memberId the id of the member being tested
     * @return true if memberId is the owner, false otherwise.
     * @throws ServiceException thrown if PersistenceException is encountered
     */
    protected boolean isOwner (int projectId, int memberId)
        throws ServiceException
    {
        try {
            return MsoyServer.swiftlyRepo.isOwner(projectId, memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Checking project ownership failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }
}
