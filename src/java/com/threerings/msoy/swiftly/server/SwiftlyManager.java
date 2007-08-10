//
// $Id$

package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.samskivert.util.SerialExecutor;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.swiftly.client.SwiftlyService;
import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

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
    public void enterProject (final ClientObject caller, final int projectId,
                              final SwiftlyService.ResultListener listener)
        throws InvocationException
    {
        ProjectRoomManager curmgr = _managers.get(projectId);
        if (curmgr != null) {
            // verify the caller has at least read permissions on the resolved room manager
            curmgr.requireReadPermissions(caller);
            listener.requestProcessed(curmgr.getPlaceObject().getOid());
            return;
        }

        final ProjectRoomManager mgr;
        ProjectRoomConfig config = new ProjectRoomConfig();
        try {
            config.projectId = projectId;
            mgr = (ProjectRoomManager)MsoyServer.plreg.createPlace(config);
            _managers.put(projectId, mgr);

            log.info("Created project room [project=" + projectId +
                     ", room=" + mgr.getPlaceObject().getOid() + "].");

        } catch (InstantiationException e) {
            log.log(Level.WARNING, "Failed to create project room [config=" + config + "].", e);
            throw new InvocationException(SwiftlyCodes.E_INTERNAL_ERROR);
        }

        // Load the project storage on the invoker thread, initialize the ProjectRoomManager
        MsoyServer.invoker.postUnit(new Invoker.Unit("loadProjectStorage") {
            public boolean invoke () {
                try {
                    // first load the project record
                    SwiftlyProjectRecord projectRecord =
                        MsoyServer.swiftlyRepo.loadProject(projectId);
                    if (projectRecord == null) {
                        throw new PersistenceException("Failed to load project record [projectId=" +
                            projectId + "].");
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
                    listener.requestFailed(SwiftlyCodes.E_INTERNAL_ERROR);
                    // remove the manager from the list since it is not fully resolved and we know
                    // this was the first user trying to resolve it.
                    mgr.shutdown();
                    return;
                }

                // verify the user has rights to at the very least read this project either because
                // the project is remixable or they are a collaborator on the project
                MemberObject memobj = (MemberObject)caller;
                if (!(_collaborators.contains(memobj.memberName) || _project.remixable)) {
                    listener.requestFailed(SwiftlyCodes.E_ACCESS_DENIED);
                    // remove the manager from the list since it is not fully resolved and we know
                    // this was the first user trying to resolve it.
                    mgr.shutdown();
                    return;
                }

                // all the necessary bits of data have been loaded, initialize the room manager
                mgr.init(_project, _collaborators, _storage);
                listener.requestProcessed(mgr.getPlaceObject().getOid());
            }

            protected SwiftlyProject _project;
            protected ProjectStorage _storage;
            protected List<MemberName> _collaborators;
            protected Exception _error;
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
