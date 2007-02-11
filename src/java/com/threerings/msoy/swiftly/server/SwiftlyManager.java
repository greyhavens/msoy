//
// $Id$

package com.threerings.msoy.swiftly.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.util.SerialExecutor;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.swiftly.client.SwiftlyService;
import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRepository;

import static com.threerings.msoy.Log.log;

/**
 * Handles the collection of Swiftly project information
 */
public class SwiftlyManager
    implements SwiftlyProvider
{
    /** This is used to execute potentially long running project actions (svn operations, builds)
     * serially on a separate thread so that they do not interfere with normal server operation. */
    public SerialExecutor executor;

    /**
     * Configures us with our repository.
     */
    public void init (InvocationManager invmgr, SwiftlyProjectRepository srepo)
    {
        _srepo = srepo;

        // register ourselves as handling the Swiftly invocation service
        invmgr.registerDispatcher(new SwiftlyDispatcher(this), SwiftlyCodes.SWIFTLY_GROUP);

        // create our executor
        executor = new SerialExecutor(MsoyServer.omgr);
    }

    /**
     * Retrieve a list of projects to which a user has commit privileges.
     */
    public ArrayList<SwiftlyProjectRecord> findProjects (MemberRecord mrec)
    {
        try {
            return _srepo.findProjects(mrec.memberId);
        } catch (PersistenceException e) {
            // TODO: Error handling
        }

        return null;
    }

    /**
     * Create a new project.
     */
    public void createProject (MemberRecord mrec, String projectName)
        throws PersistenceException
    {
        // TODO: Maximum number of projects?
        SwiftlyProjectRecord record = new SwiftlyProjectRecord();
        record.projectName = projectName;
        record.ownerId = mrec.memberId;
        _srepo.createProject(record);
    }

    // from interface SwiftlyProvider
    public void enterProject (ClientObject caller, int projectId,
                              SwiftlyService.ResultListener listener)
        throws InvocationException
    {
        ProjectRoomManager mgr = _managers.get(projectId);
        if (mgr != null) {
            listener.requestProcessed(mgr.getPlaceObject().getOid());
            return;
        }

        ProjectRoomConfig config = new ProjectRoomConfig();
        try {
            config.projectId = projectId;
            mgr = (ProjectRoomManager)MsoyServer.plreg.createPlace(config);
            _managers.put(projectId, mgr);

            log.info("Created project room [project=" + projectId +
                     ", room=" + mgr.getPlaceObject().getOid() + "].");
            listener.requestProcessed(mgr.getPlaceObject().getOid());

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create project room [config=" + config + "].", e);
            throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
        }
    }

    /**
     * Called by a project room manager when it has become empty and shutdown.
     */
    protected void projectDidShutdown (ProjectRoomManager mgr)
    {
        // clear the manager from our mapping
        ProjectRoomConfig config = (ProjectRoomConfig)mgr.getConfig();
        _managers.remove(config.projectId);
    }

    /** Handles persistent stuff. */
    protected SwiftlyProjectRepository _srepo;

    /** Maintains a mapping of resolved projects. */
    protected HashMap<Integer,ProjectRoomManager> _managers =
        new HashMap<Integer,ProjectRoomManager>();
}
