//
// $Id$

package com.threerings.msoy.web.server;

import java.util.logging.Level;
import static com.threerings.msoy.Log.log;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.web.Item;

import com.threerings.msoy.web.client.SwiftlyService;
import com.threerings.msoy.web.data.FriendEntry;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.WebCreds;

import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.server.persist.SwiftlyCollaboratorsRecord; 
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord; 
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;

import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides the server implementation of {@link SwiftlyService}.
 */
public class SwiftlyServlet extends MsoyServiceServlet
    implements SwiftlyService
{
    // from SwiftlyService
    public List getRemixableProjects (WebCreds creds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        ArrayList<SwiftlyProject> projects = new ArrayList<SwiftlyProject>();

        try {
            for (SwiftlyProjectRecord pRec :
                MsoyServer.swiftlyRepo.findRemixableProjects()) {
                projects.add(pRec.toSwiftlyProject());
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting user's projects failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        return projects;
    }

    // from SwiftlyService
    public List getMembersProjects (WebCreds creds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        ArrayList<SwiftlyProject> projects = new ArrayList<SwiftlyProject>();

        try {
            for (SwiftlyCollaboratorsRecord cRec :
                MsoyServer.swiftlyRepo.getMemberships(memrec.memberId)) {
                projects.add(MsoyServer.swiftlyRepo.loadProject(cRec.projectId).toSwiftlyProject());
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting user's projects failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        return projects;
    }

    // from SwiftlyService
    public SwiftlyProject createProject (WebCreds creds, String projectName, byte projectType,
                                         boolean remixable)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        SwiftlyProjectRecord pRec;
        SwiftlySVNStorageRecord storeRec;

        // TODO Argument Validation
        /*
        if(!isValidName(project.name)) {
            throw new ServiceException("m.invalid_project_name");
        }
        */
        
        if(!SwiftlyProject.isValidProjectType(projectType)) {
            throw new ServiceException("m.invalid_project_type");
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
                
            // Set the creator as the first collaborator.
            MsoyServer.swiftlyRepo.joinCollaborators(pRec.projectId, memrec.memberId);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Creating new project failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
        

        // If the repository initialization fails, we do our best to roll back
        // any database modifications. Hopefully that works.
        // Oh, what I'd give for transactions!

        // Initialize the SVN storage
        try {
            // Load the template from the standard path
            File templatePath = new File(ServerConfig.serverRoot + "/data/swiftly/templates/"
                + Item.getTypeName(projectType));

            ProjectSVNStorage.initializeStorage(pRec, storeRec, templatePath);
        } catch (ProjectStorageException pse) {
            log.log(Level.WARNING, "Initializing swiftly project storage failed.", pse);
            try {
                MsoyServer.swiftlyRepo.deleteProject(pRec);                
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Deleting the partially-initialized swiftly project failed.", pe);                
            }
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }            

        return pRec.toSwiftlyProject();
    }

    // from SwiftlyService
    public void updateProject (WebCreds creds, SwiftlyProject project)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);

        // TODO Argument Validation
        /*
        if(!isValidName(project.name)) {
            throw new ServiceException("m.invalid_project_name");
        }
        */
        // verify the user is the owner
        if (!isOwner(project.projectId, memrec.memberId)) {
            throw new ServiceException(SwiftlyCodes.ACCESS_DENIED);
        }

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
    public SwiftlyProject loadProject (WebCreds creds, int projectId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);

        try {
            SwiftlyProjectRecord pRec = MsoyServer.swiftlyRepo.loadProject(projectId);
            if (pRec == null) {
                throw new ServiceException(SwiftlyCodes.E_NO_SUCH_PROJECT);
            }
            // verify the user has permissions on this project
            if (!isCollaborator(pRec.projectId, memrec.memberId)) {
                throw new ServiceException(SwiftlyCodes.ACCESS_DENIED);
            }
            return pRec.toSwiftlyProject();
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Loading project failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface SwiftlyService
    public ConnectConfig loadConnectConfig (WebCreds creds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);

        // create an applet config record
        ConnectConfig config = new ConnectConfig();
        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        return config;
    }

    // from SwiftlyService
    public List getProjectCollaborators (WebCreds creds, int projectId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
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
    public List<FriendEntry> getFriends (WebCreds creds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        try {
            return MsoyServer.memberRepo.getFriends(memrec.memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting member's friends failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from SwiftlyService
    public void leaveCollaborators (WebCreds creds, int projectId, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        // verify the user is the owner
        if (!isOwner(projectId, memrec.memberId)) {
            throw new ServiceException(SwiftlyCodes.ACCESS_DENIED);
        }
        // Don't let the owner remove themselves.
        if (isOwner(projectId, memberId)) {
            return;
        }
        try {
            MsoyServer.swiftlyRepo.leaveCollaborators(projectId, memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Removing project's collaborators failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from SwiftlyService
    public void joinCollaborators (WebCreds creds, int projectId, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        // verify the user is the owner
        if (!isOwner(projectId, memrec.memberId)) {
            throw new ServiceException(SwiftlyCodes.ACCESS_DENIED);
        }
        // if the user is already a collaborator, do nothing
        if (isCollaborator(projectId, memberId)) {
            return;
        }
        try {
            MsoyServer.swiftlyRepo.joinCollaborators(projectId, memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Joining project's collaborators failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    protected boolean isCollaborator (int projectId, int memberId)
        throws ServiceException
    {
        try {
            return (MsoyServer.swiftlyRepo.getMembership(projectId, memberId) != null);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Checking project membership failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

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
