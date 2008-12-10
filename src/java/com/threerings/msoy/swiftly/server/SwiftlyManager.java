//
// $Id$

package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.web.gwt.ConnectConfig;
import com.threerings.msoy.web.server.ServletWaiter;
import com.threerings.msoy.web.server.UploadFile;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.msoy.swiftly.server.persist.SwiftlyRepository;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;

/**
 * Handles the collection of Swiftly project information
 */
@Singleton @EventThread
public class SwiftlyManager
    implements SwiftlyProvider, ShutdownManager.Shutdowner
{
    /** This thread pool is used to execute potentially long running project builds on separate
     * threads so that they do not interfere with normal server operation. */
    public ExecutorService buildExecutor;

    @Inject public SwiftlyManager (ShutdownManager shutmgr, InvocationManager invmgr)
    {
        shutmgr.registerShutdowner(this);
        invmgr.registerDispatcher(new SwiftlyDispatcher(this), SwiftlyCodes.SWIFTLY_GROUP);
    }

    /**
     * Initializes the manager and prepares it for operation.
     */
    public void init (InvocationManager invmgr)
    {
        // create our build thread pool
        buildExecutor = Executors.newFixedThreadPool(MAX_BUILD_THREADS);
    }

    /**
     * Called from SwiftlyServlet to resolve a room manager for the supplied project.
     * Whoever resolved the SwiftlyProject should have checked to make sure the supplied user
     * has permissions to load this project.
     */
    public void resolveRoomManager (MemberName name, final SwiftlyProject project,
                                    final ServletWaiter<ConnectConfig> waiter)
    {
        ProjectRoomManager curmgr = _managers.get(project.projectId);
        // the room is resolved on this node, so return this node's ConnectConfig
        if (curmgr != null) {
            waiter.requestCompleted(getConnectConfig());
            return;
        }

        // we don't have the project resolved. is it already hosted on another node?
        ConnectConfig config = _peerMan.getProjectConnectConfig(project.projectId);
        if (config != null) {
            log.info(
                "Redirecting Swiftly connection to another node. [server=" + config.server + "].");
            waiter.requestCompleted(config);
            return;
        }

        // nobody has this project resolved. let's create and host the project room on this node.
        NodeObject.Lock lock = MsoyPeerManager.getProjectLock(project.projectId);
        PeerManager.LockedOperation createOp = new PeerManager.LockedOperation() {
            public void run () {
                log.info("Got lock, creating project " + project.projectId);
                createRoom(project, waiter);
            }

            // if we failed to acquire a lock, attempt to redirect to the resolving host
            public void fail (String peerName) {
                if (peerName != null) {
                    ConnectConfig config = new ConnectConfig();
                    config.server = _peerMan.getPeerPublicHostName(peerName);
                    config.port = _peerMan.getPeerPort(peerName);
                    config.httpPort = _peerMan.getPeerHttpPort(peerName);
                    log.info("Sending Swiftly user to " + config.server + ":" + config.port + ".");
                    waiter.requestCompleted(config);
                    return;

                } else {
                    log.warning("Project lock acquired by null? [id=" + project.projectId + "].");
                    waiter.requestFailed(new Exception("Project lock acquired by null?"));
                    return;
                }
            }
        };
        _peerMan.performWithLock(lock, createOp);
    }

    /**
     * Called by a swiftly node action when a project was updated on any node.
     */
    public void projectUpdated (final SwiftlyProject project)
    {
        ProjectRoomManager manager = _managers.get(project.projectId);
        // this node is hosting the manager, send the message to the room manager
        if (manager != null) {
            manager.updateProject(project);
        }
    }

    /**
     * Called by a swiftly node action when a collaborator was added on some node.
     */
    public void collaboratorAdded (final int projectId, final MemberName name)
    {
        ProjectRoomManager manager = _managers.get(projectId);
        // this node is hosting the manager, send the message to the room manager
        if (manager != null) {
            manager.addCollaborator(name);
        }
    }

    /**
     * Called by a swiftly node action when a collaborator was removed on some node.
     */
    public void collaboratorRemoved (final int projectId, final MemberName name)
    {
        ProjectRoomManager manager = _managers.get(projectId);
        // this node is hosting the manager, send the message to the room manager
        if (manager != null) {
            manager.removeCollaborator(name);
        }
    }

    // from interface SwiftlyProvider
    public void enterProject (final ClientObject caller, final int projectId,
                              final InvocationService.ResultListener listener)
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

    // from interface ShutdownManager.Shutdowner
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
     * Informs the SwiftlyManager that the SwiftlyProject record has been changed and that change
     * should be communicated to the ProjectRoomManager on whichever node is hosting it.
     */
    public void updateProject (SwiftlyProject project, ResultListener<Void> lner)
    {
        _nodeActions.projectUpdated(project);
        lner.requestCompleted(null);
    }

    /**
     * Informs the SwiftlyManager that a collaborator has been added to a project and that change
     * should be communicated to the ProjectRoomManager on whichever node is hosting it.
     */
    public void addCollaborator (int projectId, MemberName name, ResultListener<Void> lner)
    {
        _nodeActions.collaboratorAdded(projectId, name);
        lner.requestCompleted(null);
    }

    /**
     * Informs the SwiftlyManager that a collaborator has been removed from a project and that
     * change should be communicated to the ProjectRoomManager on whichever node is hosting it.
     */
    public void removeCollaborator (int projectId, MemberName name, ResultListener<Void> lner)
    {
        _nodeActions.collaboratorRemoved(projectId, name);
        lner.requestCompleted(null);
    }

    /**
     * Informs the SwiftlyManager that a file has been uploaded and needs to be dispatched to the
     * correct room manager for processing.
     * TODO: Make this method node aware in some way.
     */
    public void insertUploadFile (int projectId, UploadFile uploadFile, ResultListener<Void> lner)
    {
        ProjectRoomManager manager = _managers.get(projectId);
        // this node is not hosting the manager, bail out
        if (manager == null) {
            return;
        }

        manager.insertUploadFile(uploadFile, lner);
    }

    /**
     * Called by a project room manager when it has become empty and shutdown.
     */
    protected void projectDidShutdown (ProjectRoomManager mgr)
    {
        ProjectRoomConfig config = (ProjectRoomConfig)mgr.getConfig();

        // remove this node as the project host from the peer manager
        _peerMan.projectDidShutdown(config.projectId);

        // clear the manager from our mapping
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

    /**
     * Create a ProjectRoomManager on this node. Failure and success will be handled using the
     * supplied ServletWaiter
     */
    protected void createRoom (final SwiftlyProject project,
                               final ServletWaiter<ConnectConfig> waiter)
    {
        final ProjectRoomManager mgr;
        ProjectRoomConfig config = new ProjectRoomConfig();
        try {
            config.projectId = project.projectId;
            mgr = (ProjectRoomManager)_plreg.createPlace(config);
            _managers.put(project.projectId, mgr);

            log.info("Created project room [project=" + project.projectId +
                ", room=" + mgr.getPlaceObject().getOid() + "].");

            // Load the project storage on the invoker thread, initialize the ProjectRoomManager
            _invoker.postUnit(new Invoker.Unit("loadProjectStorage") {
                public boolean invoke () {
                    try {
                        _project = project;

                        // load the storage record
                        SwiftlySVNStorageRecord storageRecord =
                            _swiftlyRepo.loadStorageRecordForProject(project.projectId);
                        if (storageRecord == null) {
                            throw new Exception("Project missing storage record " +
                                " [projectId=" + project.projectId + "].");
                        }
                        _storage = new ProjectSVNStorage(_project, storageRecord);

                        // and the list of collaborators
                        _collaborators = new ArrayList<MemberName>();
                        for (MemberRecord mRec : _swiftlyRepo.getCollaborators(project.projectId)) {
                            _collaborators.add(mRec.getName());
                        }
                        if (_collaborators.size() <= 0) {
                            throw new Exception("No collaborators found for project " +
                                " [projectId=" + project.projectId + "].");
                        }

                    } catch (Exception e) {
                        _error = e;
                    }

                    return true;
                }

                public void handleResult () {
                    if (_error != null) {
                        log.warning("Failed initializing room manager. ", _error);
                        waiter.requestFailed(_error);
                        // remove the manager from the list since it is not fully resolved and we
                        // know this was the first user trying to resolve it.
                        mgr.shutdown();
                        return;
                    }

                    // all the necessary bits of data have been loaded, initialize the room manager
                    mgr.init(_project, _collaborators, _swiftlyRepo, _storage,
                             getConnectConfig(), waiter);
                }

                protected SwiftlyProject _project;
                protected ProjectStorage _storage;
                protected List<MemberName> _collaborators;
                protected Exception _error;
            });

        } catch (InstantiationException e) {
            log.warning("Failed to instantiate project room [config=" + config + "].", e);
            waiter.requestFailed(e);
            return;

        } catch (InvocationException e) {
            log.warning("Failed to create project room [config=" + config + "].", e);
            waiter.requestFailed(e);
            return;
        }
    }

    /** Repository of Swiftly data. */
    @Inject protected SwiftlyRepository _swiftlyRepo;

    /** The invoker on which we do our database business. */
    @Inject protected @MainInvoker Invoker _invoker;

    /** Provides peer related services. */
    @Inject protected MsoyPeerManager _peerMan;

    /** Used to notify peers of project changes. */
    @Inject protected SwiftlyNodeActions _nodeActions;

    /** Used to create rooms. */
    @Inject protected PlaceRegistry _plreg;

    /** Maximum number of concurrent builds. */
    protected static final int MAX_BUILD_THREADS = 5;

    /** Maintains a mapping of resolved projects. */
    protected Map<Integer,ProjectRoomManager> _managers = Maps.newHashMap();
}
