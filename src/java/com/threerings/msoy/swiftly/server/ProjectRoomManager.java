//
// $Id$

package com.threerings.msoy.swiftly.server;

import java.io.IOException;

import com.samskivert.util.SerialExecutor;
import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.swiftly.client.ProjectRoomService;
import com.threerings.msoy.swiftly.data.DocumentElement;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
import com.threerings.msoy.swiftly.data.ProjectRoomMarshaller;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;

/**
 * Manages a Swiftly project room.
 */
public class ProjectRoomManager extends PlaceManager
    implements ProjectRoomProvider
{
    // from interface ProjectRoomProvider
    public void addPathElement (ClientObject caller, PathElement element)
    {
        // TODO: check access!

        // for now just update the room object
        _roomObj.addPathElement(element);
    }

    // from interface ProjectRoomProvider
    public void updatePathElement (ClientObject caller, PathElement element)
    {
        // TODO: check access!
        _roomObj.updateElements(element);
    }

    // from interface ProjectRoomProvider
    public void deletePathElement (ClientObject caller, int elementId)
    {
        // TODO: check access!
        _roomObj.removeFromElements(elementId);
    }

    // from interface ProjectRoomManager
    public void buildProject (ClientObject caller)
    {
        // TODO: check access!

        // issue a request on the executor to build this project; any log output should be
        // collected, then published on _roomObj.console
        MsoyServer.swiftlyMan.executor.addTask(new BuildProjectTask());
    }

    // from interface ProjectRoomManager
    public void commitProject (ClientObject caller, String commitMsg,
                               ProjectRoomService.ConfirmListener listener)
        throws InvocationException
    {
        // TODO: check access!

        // TODO: run the commit on the executor and post the result to the listener on success or
        // failure
        throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new ProjectRoomObject();
    }
    
    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // get a casted reference to our room object
        _roomObj = (ProjectRoomObject)_plobj;

        // Initialize a reference to the project storage
        // XXX XXX XXX XXX
        // This is certainly the wrong thread, not to mention method, in which to run these operations,
        // and I haven't the foggiest idea as to how to deal with errors. In fact, this is a HUGE HACK
        // and I promise that I'll find the right place for it very soon. In the meantime I just want
        // to see a subversion repository loaded into a swiftly context.
        try {
            SwiftlyProjectRecord projectRecord =
                MsoyServer.swiftlyRepo.loadProject(((ProjectRoomConfig)_config).projectId);
            SwiftlySVNStorageRecord storageRecord =
                MsoyServer.swiftlyRepo.loadStorageRecordForProject(((ProjectRoomConfig)_config).projectId);
            ProjectStorage storage = new ProjectSVNStorage(projectRecord, storageRecord);

            // Load up the project information and populate the room object
            for (PathElement element : storage.getProjectTree()) {
                System.out.println("Adding element: " + element);
                _roomObj.addPathElement(element);
            }
        } catch (Exception e) {
            // TODO: FIXME
            // HUGE HUGE HUGE HACK!
            System.out.println("This chick is toast! " + e);
            e.printStackTrace();
        }

        // wire up our invocation service
        _roomObj.setService((ProjectRoomMarshaller)
                            MsoyServer.invmgr.registerDispatcher(new ProjectRoomDispatcher(this)));
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        super.didShutdown();
        MsoyServer.swiftlyMan.projectDidShutdown(this);
    }

    /** Handles a request to build our project. */
    protected class BuildProjectTask implements SerialExecutor.ExecutorTask
    {
        public BuildProjectTask () {
            _projectId = ((ProjectRoomConfig)_config).projectId;
        }

        public boolean merge (SerialExecutor.ExecutorTask other) {
            // we don't want more than one pending build for a project
            if (other instanceof BuildProjectTask) {
                return _projectId == ((BuildProjectTask)other)._projectId;
            }
            return false;
        }

        public long getTimeout () {
            return 60 * 1000L; // 60 seconds is all you get kid
        }

        // this is called on the executor thread and can go hog wild with the blocking
        public void executeTask () {
            try {
                throw new IOException("Building is not yet implemented."); // TODO

            } catch (Throwable error) {
                // we'll report this on resultReceived()
                _error = error;
            }
        }

        // this is called back on the dobj thread and must only report results
        public void resultReceived () {
            if (_error != null) {
                _roomObj.setConsole(MessageBundle.tcompose("m.build_failed", _error.getMessage()));
            } else {
                _roomObj.setConsole("m.build_complete");
            }
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut () {
            _roomObj.setConsole("m.build_timed_out");
        }

        protected int _projectId;
        protected Throwable _error;
    }

    protected ProjectRoomObject _roomObj;
}
