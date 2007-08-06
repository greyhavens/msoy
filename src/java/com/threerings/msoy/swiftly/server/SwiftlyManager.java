//
// $Id$

package com.threerings.msoy.swiftly.server;

import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.SerialExecutor;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.swiftly.client.SwiftlyService;
import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.web.data.SwiftlyProject;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlyCollaboratorsRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;

import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;

import static com.threerings.msoy.Log.log;

/**
 * Handles the collection of Swiftly project information
 */
public class SwiftlyManager
    implements SwiftlyProvider, MsoyServer.Shutdowner
{
    /** This is used to execute potentially long running project builds serially on a separate
     * thread so that they do not interfere with normal server operation. */
    public SerialExecutor buildExecutor;

    /** This is used to execute potentially long running svn operations serially on a separate
     * thread so that they do not interfere with normal server operation. */
    public SerialExecutor svnExecutor;

    /**
     * Configures us with our repository.
     */
    public void init (InvocationManager invmgr)
    {
        // register ourselves as handling the Swiftly invocation service
        invmgr.registerDispatcher(new SwiftlyDispatcher(this), SwiftlyCodes.SWIFTLY_GROUP);

        // create our executors
        buildExecutor = new SerialExecutor(MsoyServer.omgr);
        svnExecutor = new SerialExecutor(MsoyServer.omgr);

        // register to be informed when the server shuts down
        MsoyServer.registerShutdowner(this);
    }

    // from interface SwiftlyProvider
    public void enterProject (ClientObject caller, final int projectId,
                              final SwiftlyService.ResultListener listener)
        throws InvocationException
    {
        ProjectRoomManager curmgr = _managers.get(projectId);
        final ProjectRoomManager mgr;
        if (curmgr != null) {
            listener.requestProcessed(curmgr.getPlaceObject().getOid());
            return;
        }

        ProjectRoomConfig config = new ProjectRoomConfig();
        try {
            config.projectId = projectId;
            mgr = (ProjectRoomManager)MsoyServer.plreg.createPlace(config);
            _managers.put(projectId, mgr);

            log.info("Created project room [project=" + projectId +
                     ", room=" + mgr.getPlaceObject().getOid() + "].");


        } catch (InstantiationException e) {
            log.log(Level.WARNING, "Failed to create project room [config=" + config + "].", e);
            throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
        }

        // Load the project storage on the invoker thread, initialize the ProjectRoomManager
        MsoyServer.invoker.postUnit(new Invoker.Unit("loadProjectStorage") {
            public boolean invoke () {
                try {
                    SwiftlyProjectRecord projectRecord =
                        MsoyServer.swiftlyRepo.loadProject(projectId);
                    if (projectRecord == null) {
                        log.warning("Failed to load project record [projectId=" +
                            projectId + "].");
                        return false;
                    }
                    _project = projectRecord.toSwiftlyProject();

                    SwiftlySVNStorageRecord storageRecord =
                        MsoyServer.swiftlyRepo.loadStorageRecordForProject(projectId);
                    if (storageRecord == null) {
                        log.warning("Project missing storage record [projectId=" +
                            projectId + "].");
                        return false;
                    }
                    _storage = new ProjectSVNStorage(_project, storageRecord);

                    _collaborators = new HashIntMap<SwiftlyCollaboratorsRecord>();
                    for (SwiftlyCollaboratorsRecord record :
                        MsoyServer.swiftlyRepo.getCollaborators(projectId)) {
                        _collaborators.put(record.memberId, record);
                    }
                    return true;

                } catch (ProjectStorageException pse) {
                    log.log(Level.WARNING, "Failed to open swiftly project storage [projectId=" +
                        projectId + "].", pse);
                    return false;

                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to find project storage record [projectId=" +
                        projectId + "].", pe);
                    return false;
                }
            }

            public void handleResult () {
                mgr.init(_project, _collaborators, _storage);
                listener.requestProcessed(mgr.getPlaceObject().getOid());
            }

            protected SwiftlyProject _project;
            protected ProjectStorage _storage;
            protected HashIntMap<SwiftlyCollaboratorsRecord> _collaborators;
        });
    }

    // from interface MsoyServer.Shutdowner
    public void shutdown ()
    {
        // we need to shut down any active room managers to ensure they flush their bits
        for (ProjectRoomManager mgr : _managers.values()) {
            mgr.shutdown();
        }

        // TODO: wait for our serial executors to finish, but timeout if they take more than 90
        // seconds or so
    }

    /**
     * Return the ProjectRoomManager for the given projectId. May return null.
     */
    public ProjectRoomManager getRoomManager (int projectId)
    {
        return _managers.get(projectId);
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

    /** Maintains a mapping of resolved projects. */
    protected HashMap<Integer,ProjectRoomManager> _managers =
        new HashMap<Integer,ProjectRoomManager>();
}
