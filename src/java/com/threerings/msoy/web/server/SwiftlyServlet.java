//
// $Id$

package com.threerings.msoy.web.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;

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
        final MemberRecord memrec = requireAuthedUser(creds);
        final ServletWaiter<ArrayList<SwiftlyProject>> waiter;

        waiter = new ServletWaiter<ArrayList<SwiftlyProject>>("getProjects[]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<ArrayList<SwiftlyProject>>(waiter) {
            public ArrayList<SwiftlyProject> invokePersistResult () throws PersistenceException {
                ArrayList<SwiftlyProject> projects = new ArrayList<SwiftlyProject>();
                for (SwiftlyProjectRecord pRec : MsoyServer.swiftlyRepo.findProjects(memrec.memberId)) {
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
        MemberRecord memrec = requireAuthedUser(creds);
        final ServletWaiter<SwiftlyProject> waiter;

        /*
        if(!isValidName(project.name)) {
            throw new ServiceException("m.invalid_project_name");
        }
        */

        // TODO: project.creationDate = new Date(System.currentTimeMillis());
        project.ownerId = creds.getMemberId();
        waiter = new ServletWaiter<SwiftlyProject>("createProject[" + project + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.swiftlyMan.createProject(project, waiter);
            }
        });
        return waiter.waitForResult();
    }
}
