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

import com.threerings.msoy.web.client.SwiftlyService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.SwiftlyConfig;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.SwiftlyProjectType;
import com.threerings.msoy.web.data.WebCreds;

import com.threerings.msoy.swiftly.server.persist.SwiftlyCollaboratorsRecord; 
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord; 
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectTypeRecord; 

import java.util.ArrayList;
import java.util.List;

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
    public List getProjectTypes (WebCreds creds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        ArrayList<SwiftlyProjectType> types = new ArrayList<SwiftlyProjectType>();

        try {
            for (SwiftlyProjectTypeRecord tRec :
                MsoyServer.swiftlyRepo.getProjectTypes(memrec.memberId)) {
                types.add(tRec.toSwiftlyProjectType());
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting user's project types failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        return types;
    }

    // from SwiftlyService
    public SwiftlyProject createProject (WebCreds creds, String projectName, int projectType,
                                         boolean remixable)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);

        // TODO Argument Validation
        /*
        if(!isValidName(project.name)) {
            throw new ServiceException("m.invalid_project_name");
        }
        */

        try {
            SwiftlyProject project = MsoyServer.swiftlyRepo.createProject(
                memrec.memberId, projectName, projectType, remixable).toSwiftlyProject();
            // the creator is the first member of the collaborators
            MsoyServer.swiftlyRepo.joinCollaborators(project.projectId, memrec.memberId); 
            return project;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Creating new project failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from SwiftlyService
    public SwiftlyProject loadProject (WebCreds creds, int projectId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);

        try {
            // TODO: verify the user has permissions on this project
            // TODO: loadProject can return null if a project is not found for check for that
            return MsoyServer.swiftlyRepo.loadProject(projectId).toSwiftlyProject();
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Loading project failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface SwiftlyService
    public SwiftlyConfig loadSwiftlyConfig (WebCreds creds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);

        // create an applet config record
        SwiftlyConfig config = new SwiftlyConfig();

        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        return config;
    }
}
