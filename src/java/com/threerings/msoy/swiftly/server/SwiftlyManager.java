//
// $Id$

package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.server.ServletWaiter;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.client.InvocationService.ResultListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

/**
 * Handles the collection of Swiftly project information
 */
public class SwiftlyManager
    implements SwiftlyProvider, MsoyServer.Shutdowner
{
    /** This thread pool is used to execute potentially long running project builds on separate
     * threads so that they do not interfere with normal server operation. */
    public ExecutorService buildExecutor;

    /**
     * Configures us with our repository.
     */
    public void init (InvocationManager invmgr)
    {
        // register ourselves as handling the Swiftly invocation service
        invmgr.registerDispatcher(new SwiftlyDispatcher(this), SwiftlyCodes.SWIFTLY_GROUP);

        // create our build thread pool
        buildExecutor = Executors.newFixedThreadPool(MAX_BUILD_THREADS);

        // register to be informed when the server shuts down
        MsoyServer.registerShutdowner(this);
    }


    public void resolveRoomManager (MemberName name, final int projectId,
                                    final ServletWaiter<ConnectConfig> waiter)
    {
        ProjectRoomManager curmgr = _managers.get(projectId);
        // the room is resolved on this node, so return this node's ConnectConfig
        if (curmgr != null) {
            // verify the caller has at least read permissions on the resolved room manager
            // TODO: need a MemberObject curmgr.requireReadPermissions(caller);

            waiter.requestCompleted(getConnectConfig());
            return;
        }

        // TODO: lookup in the peer manager whether anyone has this room already resolved. use that
        // nodes connect config if it is already resolved.

        // TODO: perform this operation with a peer lock and insert this manager as the resolver
        // of this project into the peermanager.

        // the room manager is not resolved on any node, so resolve it on this node
        final ProjectRoomManager mgr;
        ProjectRoomConfig config = new ProjectRoomConfig();
        try {
            config.projectId = projectId;
            mgr = (ProjectRoomManager)MsoyServer.plreg.createPlace(config);
            _managers.put(projectId, mgr);

            log.info("Created project room [project=" + projectId +
                     ", room=" + mgr.getPlaceObject().getOid() + "].");

            // Load the project storage on the invoker thread, initialize the ProjectRoomManager
            MsoyServer.invoker.postUnit(new Invoker.Unit("loadProjectStorage") {
                public boolean invoke () {
                    try {
                        // first load the project record
                        SwiftlyProjectRecord projectRecord =
                            MsoyServer.swiftlyRepo.loadProject(projectId);
                        if (projectRecord == null) {
                            throw new PersistenceException("Failed to load project record " +
                                "[projectId=" + projectId + "].");
                        }
                        _project = projectRecord.toSwiftlyProject();

                        // then the storage record
                        SwiftlySVNStorageRecord storageRecord =
                            MsoyServer.swiftlyRepo.loadStorageRecordForProject(projectId);
                        if (storageRecord == null) {
                            throw new PersistenceException("Project missing storage record " +
                                " [projectId=" + projectId + "].");
                        }
                        _storage = new ProjectSVNStorage(_project, storageRecord);

                        // and finally the list of collaborators
                        _collaborators = new ArrayList<MemberName>();
                        for (MemberRecord mRec :
                            MsoyServer.swiftlyRepo.getCollaborators(projectId)) {
                            _collaborators.add(mRec.getName());
                        }
                        if (_collaborators.size() <= 0) {
                            throw new PersistenceException("No collaborators found for project " +
                                " [projectId=" + projectId + "].");
                        }

                    } catch (Exception e) {
                        _error = e;
                    }

                    return true;
                }

                public void handleResult () {
                    if (_error != null) {
                        log.log(Level.WARNING, "Failed initializing room manager. ", _error);
                        waiter.requestFailed(_error);
                        // remove the manager from the list since it is not fully resolved and we
                        // know this was the first user trying to resolve it.
                        mgr.shutdown();
                        return;
                    }

                    // if it was cheap and easy, we would check the users rights at this point to
                    // make sure they should be able to resolve the room manager. but the cleanest
                    // way to do that would be after the mgr.init() which is not a free operation.
                    // we could redefine what read access means at this point, but that's just
                    // nasty. so we'll rely on all the service checks we have in the manager to
                    // protect us.

                    // all the necessary bits of data have been loaded, initialize the room manager
                    mgr.init(_project, _collaborators, _storage, new ConfirmListener() {
                        public void requestProcessed ()
                        {
                            waiter.requestCompleted(getConnectConfig());
                        }

                        public void requestFailed (String cause)
                        {
                            waiter.requestFailed(new Exception(cause));
                        }
                    });
                }

                protected SwiftlyProject _project;
                protected ProjectStorage _storage;
                protected List<MemberName> _collaborators;
                protected Exception _error;
            });

        } catch (InstantiationException e) {
            log.log(Level.WARNING,
                "Failed to instantiate project room [config=" + config + "].", e);
            waiter.requestFailed(e);
            return;

        } catch (InvocationException e) {
            log.log(Level.WARNING, "Failed to create project room [config=" + config + "].", e);
            waiter.requestFailed(e);
            return;
        }

    }

    // from interface SwiftlyProvider
    public void enterProject (final ClientObject caller, final int projectId,
                              final ResultListener listener)
        throws InvocationException
    {
        ProjectRoomManager curmgr = _managers.get(projectId);
        // we did not find a room manager in our resolved list, which should have been resolved
        // by the GWT client asking for the ConnectConfig, so fail the request.
        if (curmgr == null) {
            log.warning("Failed to enter project. Manager not resolved. [projectId=" +
                projectId + "].");
            listener.requestFailed(SwiftlyCodes.E_INTERNAL_ERROR);
            return;
        }

        // verify the caller has at least read permissions on the resolved room manager
        curmgr.requireReadPermissions(caller);
        listener.requestProcessed(curmgr.getPlaceObject().getOid());
        return;
    }

    // from interface MsoyServer.Shutdowner
    public void shutdown ()
    {
        // we need to shut down any active room managers to ensure they flush their bits
        for (ProjectRoomManager mgr : _managers.values()) {
            mgr.shutdown();
        }

        // shutdown the build executor and give it 20 seconds to complete running builds.
        buildExecutor.shutdown();
        try {
            buildExecutor.awaitTermination(20L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // So we got interrupted. Let's just let the server shutdown at this point.
        }
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

    /**
     * Returns the ConnectConfig for this node.
     */
    protected ConnectConfig getConnectConfig ()
    {
        ConnectConfig config = new ConnectConfig();
        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        config.httpPort = ServerConfig.httpPort;
        return config;
    }

    /** Maximum number of concurrent builds. */
    protected static final int MAX_BUILD_THREADS = 5;

    /** Maintains a mapping of resolved projects. */
    protected HashMap<Integer,ProjectRoomManager> _managers =
        new HashMap<Integer,ProjectRoomManager>();
}
