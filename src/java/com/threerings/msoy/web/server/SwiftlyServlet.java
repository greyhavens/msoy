//
// $Id$

package com.threerings.msoy.web.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.SwiftlyService;
import com.threerings.msoy.web.data.ServiceException;
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
        final ServletWaiter<ArrayList<SwiftlyProject>> waiter =
            new ServletWaiter<ArrayList<SwiftlyProject>>("getProjects[]");
        final int memberId = creds.getMemberId();
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<ArrayList<SwiftlyProject>>(waiter) {
            public ArrayList<SwiftlyProject> invokePersistResult () throws PersistenceException {
                ArrayList<SwiftlyProject> projects = new ArrayList<SwiftlyProject>();
                for (SwiftlyProjectRecord pRec : MsoyServer.swiftlyRepo.findProjects(memberId)) {
                    projects.add(pRec.toSwiftlyProject());
                }
                return projects;
            }
        });
        return waiter.waitForResult();
    }

    // from SwiftlyService
    public SwiftlyProject createProject (WebCreds creds, final SwiftlyProject project)
        throws ServiceException
    {
        // TODO: validate creds
        /*
        if(!isValidName(project.name)) {
            throw new ServiceException("m.invalid_project_name");
        }
        */

        final ServletWaiter<SwiftlyProject> waiter =
            new ServletWaiter<SwiftlyProject>("createProject[" + project + "]");
        // TODO: project.creationDate = new Date(System.currentTimeMillis());
        project.ownerId = creds.getMemberId();
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.swiftlyMan.createProject(project, waiter);
            }
        });
        return waiter.waitForResult();
    }
}
