//
// $Id$

package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import com.samskivert.jdbc.depot.DuplicateKeyException;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.msoy.swiftly.gwt.SwiftlyConnectConfig;
import com.threerings.msoy.swiftly.gwt.SwiftlyService;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlyRepository;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;

/**
 * Provides the server implementation of {@link SwiftlyService}.
 */
public class SwiftlyServlet extends MsoyServiceServlet
    implements SwiftlyService
{
    // from SwiftlyService
    public SwiftlyConnectConfig getConnectConfig (final int projectId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();

        // load the project. this also verifies the user has permissions to view the project
        final SwiftlyProject project = loadProject(projectId);

        // run a task on the dobject thread that finds the ProjectRoomManager for this project
        // either on this server or on a different node and returns that server's ConnectConfig
        final ServletWaiter<ConnectConfig> waiter =
            new ServletWaiter<ConnectConfig>("resolveRoomManager[" + projectId + "]");
        _omgr.postRunnable(new Runnable() {
            public void run () {
                _swiftlyMan.resolveRoomManager(memrec.getName(), project, waiter);
            }
        });

        // block the servlet waiting for the dobject thread
        waiter.waitForResult();

        return new SwiftlyConnectConfig(waiter.getArgument(), project);
    }

    // from SwiftlyService
    public List<SwiftlyProject> getRemixableProjects ()
        throws ServiceException
    {
        requireAuthedUser();
        ArrayList<SwiftlyProject> projects = new ArrayList<SwiftlyProject>();
        for (SwiftlyProjectRecord pRec : _swiftlyRepo.findRemixableProjects()) {
            projects.add(pRec.toSwiftlyProject());
        }
        return projects;
    }

    // from SwiftlyService
    public List<SwiftlyProject> getMembersProjects ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        ArrayList<SwiftlyProject> projects = new ArrayList<SwiftlyProject>();
        for (SwiftlyProjectRecord pRec : _swiftlyRepo.findMembersProjects(memrec.memberId)) {
            projects.add(pRec.toSwiftlyProject());
        }
        return projects;
    }

    // from SwiftlyService
    public SwiftlyProject createProject (String projectName, byte projectType, boolean remixable)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();

        SwiftlyProject project;
        SwiftlyProjectRecord pRec;
        SwiftlySVNStorageRecord storeRec;

        // TODO Argument Validation
        /*
        if (!isValidName(project.name)) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        */

        if (!SwiftlyProject.isValidProjectType(projectType)) {
            log.warning("Failed to create project. Invalid project type. [type=" +
                projectType + "]");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // Initialize the project storage.
        // XXX We need to sort out how to properly create remote repositories.
        // Until then, we create them in a hard-wired local directory.
        String svnRoot = ServerConfig.serverRoot + "/data/swiftly/projects";
        storeRec = _swiftlyRepo.createSVNStorage(ProjectSVNStorage.PROTOCOL_FILE, "", 0, svnRoot);

        // Create the project record.
        try {
            pRec = _swiftlyRepo.createProject(
                memrec.memberId, projectName, projectType, storeRec.storageId, remixable);
            project = pRec.toSwiftlyProject();

            // Set the creator as the first collaborator.
            _swiftlyRepo.joinCollaborators(pRec.projectId, memrec.memberId);

        } catch (DuplicateKeyException dke) {
            throw new ServiceException(SwiftlyCodes.E_PROJECT_NAME_EXISTS);
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
            log.warning("Initializing swiftly project storage failed.", pse);
            try {
                _swiftlyRepo.deleteProject(pRec);
            } catch (Exception e) {
                log.warning("Deleting the partially-initialized swiftly project failed.", e);
            }
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        return project;
    }

    // from SwiftlyService
    public void updateProject (SwiftlyProject project)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        requireOwner(project.projectId, memrec.memberId);

        // TODO Argument Validation
        /*
        if (!isValidName(project.name)) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        */

        SwiftlyProjectRecord pRec = _swiftlyRepo.loadProject(project.projectId);
        if (pRec == null) {
            log.warning("Cannot update non-existent project", "id", project.projectId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        Map<String, Object> updates = pRec.findUpdates(project);
        if (updates.size() > 0) {
            _swiftlyRepo.updateProject(project.projectId, updates);
            // inform the room manager, if resolved, that the project has changed
            updateRoomProject(project);
        }
    }

    // from SwiftlyService
    public void deleteProject (int projectId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        requireOwner(projectId, memrec.memberId);
        _swiftlyRepo.markProjectDeleted(projectId);
    }

    // from SwiftlyService
    public SwiftlyProject loadProject (int projectId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        SwiftlyProjectRecord pRec = _swiftlyRepo.loadProject(projectId);
        if (pRec == null) {
            throw new ServiceException(SwiftlyCodes.E_NO_SUCH_PROJECT);
        }
        // verify the user has permission to view this project
        // TODO: read access is defined here and the room object. Can we simplify this?
        if (!pRec.remixable && !isCollaborator(pRec.projectId, memrec.memberId)) {
            throw new ServiceException(SwiftlyCodes.ACCESS_DENIED);
        }
        return pRec.toSwiftlyProject();
    }

    // from SwiftlyService
    public MemberName getProjectOwner (int projectId)
        throws ServiceException
    {
        requireAuthedUser();
        MemberRecord mRec = _swiftlyRepo.loadProjectOwner(projectId);
        if (mRec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        return mRec.getName();
    }

    // from SwiftlyService
    public List<MemberName> getProjectCollaborators (int projectId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        requireCollaborator(projectId, memrec.memberId);
        ArrayList<MemberName> members = new ArrayList<MemberName>();
        for (MemberRecord mRec : _swiftlyRepo.getCollaborators(projectId)) {
            members.add(mRec.getName());
        }
        return members;
    }

    // from SwiftlyService
    public List<FriendEntry> getFriends ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        return _memberRepo.loadFriends(memrec.memberId, -1);
    }

    // from SwiftlyService
    public void leaveCollaborators (int projectId, MemberName name)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        requireOwner(projectId, memrec.memberId);

        // Don't let the owner remove themselves.
        if (isOwner(projectId, name.getMemberId())) {
            log.warning("Refusing to remove the project owner from collaborators. Aborting " +
                "request. [projectId=" + projectId + "]");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        _swiftlyRepo.leaveCollaborators(projectId, name.getMemberId());

        // inform the project room manager of the change in collaborators
        removeFromRoomCollaborators(projectId, name);
    }

    // from SwiftlyService
    public void joinCollaborators (int projectId, MemberName name)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        requireOwner(projectId, memrec.memberId);

        // if the user is already a collaborator, do nothing
        if (isCollaborator(projectId, name.getMemberId())) {
            log.warning("Refusing to add an existing collaborator to project. Aborting request. " +
                "[projectId="+ projectId + ", memberId=" + name.getMemberId() + "]");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        _swiftlyRepo.joinCollaborators(projectId, name.getMemberId());

        // inform the project room manager of the change in collaborators
        addToRoomCollaborators(projectId, name);
    }

    /**
     * Informs the room manager for this project, if resolved, that a collaborator has
     * been added.
     */
    protected void addToRoomCollaborators (final int projectId, final MemberName name)
        throws ServiceException
    {
        // run a task on the dobject thread that first finds the ProjectRoomManager for this
        // project if it exists, and then tells it to update its local list of collaborators
        final ServletWaiter<Void> waiter =
            new ServletWaiter<Void>("addCollaborator[" + projectId + "]");
        _omgr.postRunnable(new Runnable() {
            public void run () {
                _swiftlyMan.addCollaborator(projectId, name, waiter);
            }
        });

        // block the servlet waiting for the dobject thread
        waiter.waitForResult();
    }

    /**
     * Informs the room manager for this project, if resolved, that a collaborator has
     * been removed.
     */
    protected void removeFromRoomCollaborators (final int projectId, final MemberName name)
        throws ServiceException
    {
        // run a task on the dobject thread that first finds the ProjectRoomManager for this
        // project if it exists, and then tells it to update its local list of collaborators
        final ServletWaiter<Void> waiter =
            new ServletWaiter<Void>("removeCollaborator[" + projectId + "]");
        _omgr.postRunnable(new Runnable() {
            public void run () {
                _swiftlyMan.removeCollaborator(projectId, name, waiter);
            }
        });

        // block the servlet waiting for the dobject thread
        waiter.waitForResult();
    }

    /**
     * Informs the room manager for this project, if resolved, that the project has been modified.
     */
    protected void updateRoomProject (final SwiftlyProject project)
        throws ServiceException
    {
        // run a task on the dobject thread that first finds the ProjectRoomManager for this
        // project if it exists, and then tells it to update its local swiftly project
        final ServletWaiter<Void> waiter =
            new ServletWaiter<Void>("updateProject[" + project.projectId + "]");
        _omgr.postRunnable(new Runnable() {
            public void run () {
                _swiftlyMan.updateProject(project, waiter);
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
     */
    protected boolean isCollaborator (int projectId, int memberId)
        throws ServiceException
    {
        return _swiftlyRepo.isCollaborator(projectId, memberId);
    }

    /**
     * Determines if a member is the owner of the supplied project.
     * @param projectId the id of the project being tested
     * @param memberId the id of the member being tested
     * @return true if memberId is the owner, false otherwise.
     */
    protected boolean isOwner (int projectId, int memberId)
        throws ServiceException
    {
        return _swiftlyRepo.isOwner(projectId, memberId);
    }

    // our dependencies
    @Inject protected RootDObjectManager _omgr;
    @Inject protected SwiftlyManager _swiftlyMan;
    @Inject protected SwiftlyRepository _swiftlyRepo;
}
