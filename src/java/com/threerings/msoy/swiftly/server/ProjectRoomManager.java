//
// $Id$

package com.threerings.msoy.swiftly.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.samskivert.util.Invoker;
import com.samskivert.util.SerialExecutor;
import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.swiftly.client.ProjectRoomService;
import com.threerings.msoy.swiftly.data.DocumentUpdatedEvent;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
import com.threerings.msoy.swiftly.data.ProjectRoomMarshaller;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.web.data.SwiftlyProject;

import com.threerings.msoy.swiftly.server.SwiftlyManager;

import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;

/**
 * Manages a Swiftly project room.
 */
public class ProjectRoomManager extends PlaceManager
    implements ProjectRoomProvider, SetListener
{
    /**
     * Called by the {@link SwiftlyManager} after creating this project room manager.
     */
    public void init (SwiftlyProject project, ProjectStorage storage)
    {
        _storage = storage;
        _roomObj.setProject(project);

        // Load the project tree from the storage provider
        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    // Okay, so it's not really a tree, but hey ...
                    // Load the file list from the provided storage instance.
                    _projectTree = _storage.getProjectTree();
                    return true;
                } catch (ProjectStorageException pse) {
                    // TODO: Handle this how?
                    return false;
                }
            }
            
            public void handleResult () {
                for (PathElement element : _projectTree) {
                    _roomObj.addPathElement(element);
                }
            }

            protected List<PathElement> _projectTree;
        });
    }

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
        _roomObj.updatePathElements(element);
    }

    // from interface ProjectRoomProvider
    public void deletePathElement (ClientObject caller, int elementId)
    {
        // TODO: check access!
        _roomObj.removeFromPathElements(elementId);
    }

    // from interface ProjectRoomProvider
    public void addDocument (ClientObject caller, PathElement element,
                             final ProjectRoomService.InvocationListener listener)
    {
        // TODO: check access!

        // Re-bind transient instance variables
        element.lazarus(_roomObj.pathElements);

        SwiftlyDocument doc = null;
        try {
            doc = new SwiftlyDocument(element, ProjectStorage.TEXT_ENCODING);
        } catch (IOException e) {
            listener.requestFailed("e.add_document_failed");
            return;
        }

        // add the path element to the dset
        _roomObj.addPathElement(element);

        // add the swiftly document to the dest
        _roomObj.addSwiftlyDocument(doc);
    }

    // from interface ProjectRoomProvider
    public void updateDocument (ClientObject caller, int elementId, String text)
    {
        // TODO: check access!

        // set the changed flag if needed
        SwiftlyDocument doc = _roomObj.documents.get(elementId);
        if (!doc.wasChanged()) {
            doc.setChanged(true);
            _roomObj.updateDocuments(doc);
        }

        _roomObj.postEvent(new DocumentUpdatedEvent(
                               _roomObj.getOid(), caller.getOid(), elementId, text));
    }

    // from interface ProjectRoomProvider
    public void deleteDocument (ClientObject caller, int elementId)
    {
        // TODO: check access!
        _roomObj.removeFromDocuments(elementId);
    }

    // from interface ProjectRoomProvider
    public void buildProject (ClientObject caller)
    {
        // TODO: check access!

        // perform a commit first. if that works, it will run the build
        doCommit(true);
    }

    // from interface ProjectRoomProvider
    public void commitProject (ClientObject caller, String commitMsg,
                               ProjectRoomService.ConfirmListener listener)
        throws InvocationException
    {
        // TODO: check access!

        // TODO: run the commit on the executor and post the result to the listener on success or
        // failure
        throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
    }

    // from interface ProjectRoomProvider
    public void loadDocument (ClientObject caller, final PathElement element)
    {
        // TODO: check access!

        // Load the document from the storage provider
        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _doc = _storage.getDocument(element);
                    return true;
                } catch (ProjectStorageException pse) {
                    // TODO: Handle this how?
                    return false;
                }
            }
            
            public void handleResult () {
                _roomObj.addSwiftlyDocument(_doc);
            }

            protected SwiftlyDocument _doc;
        });
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            SwiftlyDocument element = (SwiftlyDocument)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);
        } else if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            PathElement element = (PathElement)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            SwiftlyDocument element = (SwiftlyDocument)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);
        } else if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            PathElement element = (PathElement)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);
        }
    }
    
    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        // nada
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

        // wire up our invocation service
        _roomObj.setService((ProjectRoomMarshaller)
                            MsoyServer.invmgr.registerDispatcher(new ProjectRoomDispatcher(this)));
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        super.didShutdown();
        doCommit(false);
        MsoyServer.swiftlyMan.projectDidShutdown(this);
    }


    /**
     * Issue a request on the executor to commit this project; any log output should be
     * collected, then published on _roomObj.console. If the commit works, it will
     * start the build if true was passed as a parameter.
     */
    protected void doCommit (boolean shouldBuild)
    {
        MsoyServer.swiftlyMan.svnExecutor.addTask(new CommitProjectTask(shouldBuild));
    }

    /** Handles a request to commit our project. */
    protected class CommitProjectTask implements SerialExecutor.ExecutorTask
    {
        public CommitProjectTask (boolean shouldBuild) {
            _projectId = ((ProjectRoomConfig)_config).projectId;
            _shouldBuild = shouldBuild;
        }

        public boolean merge (SerialExecutor.ExecutorTask other) {
            // we don't want more than one pending commit for a project
            if (other instanceof CommitProjectTask) {
                return _projectId == ((CommitProjectTask)other)._projectId;
            }
            return false;
        }

        public long getTimeout () {
            return 60 * 1000L; // 60 seconds is all you get kid
        }

        // this is called on the executor thread and can go hog wild with the blocking
        public void executeTask () {
            try {
                ArrayList<SwiftlyDocument> docs = new ArrayList<SwiftlyDocument>();
                // commit each swiftly document in the project that has changed
                for (SwiftlyDocument doc : _roomObj.documents) {
                    if (doc.isDirty()) {
                        _storage.putDocument(doc, "Committing this file");
                        docs.add(doc);
                    }
                }
                for (SwiftlyDocument doc : docs) {
                    doc.setChanged(false);
                    doc.commit();
                    _roomObj.updateDocuments(doc);
                }
            } catch (Throwable error) {
                // we'll report this on resultReceived()
                _error = error;
            }
        }

        // this is called back on the dobj thread and must only report results
        public void resultReceived () {
            if (_error != null) {
                if (_error instanceof ProjectStorageException) {
                    _roomObj.setConsole("m.commit_failed");
                } else {
                    _roomObj.setConsole(
                        MessageBundle.tcompose("m.commit_failed_unknown", _error.getMessage()));
                }
            } else {
                // if the commit work, run the build if set
                if (_shouldBuild) {
                    MsoyServer.swiftlyMan.buildExecutor.addTask(new BuildProjectTask());
                    return;
                }
                _roomObj.setConsole("m.commit_complete");
            }
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut () {
            _roomObj.setConsole("m.commit_timed_out");
        }

        protected int _projectId;
        boolean _shouldBuild;
        protected Throwable _error;
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
    protected ProjectStorage _storage;
}
