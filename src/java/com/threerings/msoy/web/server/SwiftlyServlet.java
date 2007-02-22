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
import com.threerings.msoy.web.data.WebCreds;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord; 

import java.util.ArrayList;

/**
 * Provides the server implementation of {@link SwiftlyService}.
 */
public class SwiftlyServlet extends MsoyServiceServlet
    implements SwiftlyService
{
    // from SwiftlyService
    public ArrayList getProjects (WebCreds creds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        ArrayList<SwiftlyProject> projects = new ArrayList<SwiftlyProject>();

        try {
            for (SwiftlyProjectRecord pRec : MsoyServer.swiftlyRepo.findProjects(memrec.memberId)) {
                projects.add(pRec.toSwiftlyProject());
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Getting user's projects failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        return projects;
    }

    // from SwiftlyService
    public SwiftlyProject createProject (WebCreds creds, String projectName)
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
            return MsoyServer.swiftlyRepo.createProject(memrec.memberId, projectName).toSwiftlyProject();
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Creating new project failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface SwiftlyService
    public SwiftlyConfig loadSwiftlyConfig (WebCreds creds)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // create an applet config record
        SwiftlyConfig config = new SwiftlyConfig();

        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        return config;
    }
}
