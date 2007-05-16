//
// $Id$

package com.threerings.msoy.swiftly.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Invoker;
import com.samskivert.util.SerialExecutor;
import com.samskivert.util.HashIntMap;
import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.data.DocumentUpdatedEvent;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
import com.threerings.msoy.swiftly.data.ProjectRoomMarshaller;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyBinaryDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

import com.threerings.msoy.web.data.SwiftlyProject;

import com.threerings.msoy.swiftly.server.SwiftlyManager;
import com.threerings.msoy.swiftly.server.build.LocalProjectBuilder;
import com.threerings.msoy.swiftly.server.build.ProjectBuilderException;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;

import org.apache.commons.io.FileUtils;

import static com.threerings.msoy.Log.log;

/**
 * Manages a Swiftly project room.
 */
public class ProjectRoomManager extends PlaceManager
    implements ProjectRoomProvider, SetListener
{
    /**
     * Called by the {@link SwiftlyManager} after creating this project room manager.
     */
    public void init (final SwiftlyProject project, ArrayIntSet collaborators,
                      ProjectStorage storage)
    {
        _storage = storage;
        _collaborators = collaborators;

        // References to our on-disk SDKs
        File flexSdk = new File(ServerConfig.serverRoot + FLEX_SDK);
        File whirledSdk = new File(ServerConfig.serverRoot + WHIRLED_SDK);

        // Setup the builder.
        _builder = new LocalProjectBuilder(
            project, _storage, flexSdk.getAbsoluteFile(), whirledSdk.getAbsoluteFile());

        _currentUploads = new HashIntMap<UploadFile>();

        // Load the project tree from the storage provider
        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit("loadProject") {
            public boolean invoke () {
                try {
                    // Okay, so it's not really a tree, but hey ...
                    // Load the file list from the provided storage instance.
                    _projectTree = _storage.getProjectTree();
                    return true;
                } catch (ProjectStorageException pse) {
                    // TODO: Handle this how?
                    log.log(Level.WARNING,
                        "Loading project tree failed. [project=" + project + "].", pse);
                    return false;
                }
            }

            public void handleResult () {
                for (PathElement element : _projectTree) {
                    _roomObj.addPathElement(element);
                }
                // Inform any listeners that the project has been loaded by adding it to the dobj
                _roomObj.setProject(project);
            }

            protected List<PathElement> _projectTree;
        });
    }

    // from interface ProjectRoomProvider
    public void addPathElement (ClientObject caller, PathElement element,
                                InvocationListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        // for now just update the room object
        _roomObj.addPathElement(element);
    }

    // from interface ProjectRoomProvider
    public void updatePathElement (ClientObject caller, PathElement element,
                                   InvocationListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        _roomObj.updatePathElements(element);
    }

    // from interface ProjectRoomProvider
    public void deletePathElement (ClientObject caller, final int elementId,
                                   final ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        final PathElement element = _roomObj.pathElements.get(elementId);
        if (element == null) {
            throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
        }

        // if the document associated with this element is resolved, unload it (if the removal
        // fails, this will only be a minor inconvenience)
        SwiftlyDocument doc = _roomObj.getDocument(element);
        if (doc != null) {
            _roomObj.removeFromDocuments(doc.getKey());
        }

        // if the path element was not committed to the repository, just remove it from the DSet
        // and we're done
        if (!element.inRepo) {
            _roomObj.removeFromPathElements(elementId);
            listener.requestProcessed();
            return;
        }

        // Otherwise we'll have to delete the document from the storage provider
        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit("deletePathElement") {
            public boolean invoke () {
                try {
                    _storage.deleteDocument(element, "Automatic Swiftly Delete");
                } catch (ProjectStorageException pse) {
                    _error = pse;
                }
                return true;
            }

            public void handleResult () {
                if (_error != null) {
                    log.log(Level.WARNING, "Delete pathElement failed [element=" + element + "].",
                            _error);
                    listener.requestFailed("e.delete_element_failed");
                    return;
                }
                // remove it from path elements
                _roomObj.removeFromPathElements(elementId);
                listener.requestProcessed();
            }

            protected Exception _error;
        });
    }

    // from interface ProjectRoomProvider
    public void renamePathElement (ClientObject caller, int elementId, final String newName,
                                   final ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        final PathElement element = _roomObj.pathElements.get(elementId);
        if (element == null) {
            throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
        }

        // if the path element was not committed to the repository, just rename it in the DSet
        // and we're done
        if (!element.inRepo) {
            element.setName(newName);
            _roomObj.updatePathElements(element);
            listener.requestProcessed();
            return;
        }

        // Otherwise we'll have to rename the document from the storage provider
        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit("renamePathElement") {
            public boolean invoke () {
                try {
                    _storage.renameDocument(element, newName, "Automatic Swiftly Rename");
                } catch (ProjectStorageException pse) {
                    _error = pse;
                }
                return true;
            }

            public void handleResult () {
                if (_error != null) {
                    log.log(Level.WARNING, "Rename pathElement failed [element=" + element + "].",
                            _error);
                    listener.requestFailed("e.rename_element_failed");
                    return;
                }
                // rename it in the dset
                element.setName(newName);
                _roomObj.updatePathElements(element);
                listener.requestProcessed();
            }

            protected Exception _error;
        });
    }

    // from interface ProjectRoomProvider
    public void addDocument (ClientObject caller, String fileName, PathElement parent,
                             String mimeType, final InvocationListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        PathElement element = PathElement.createFile(fileName, parent, mimeType);

        SwiftlyTextDocument doc = null;
        try {
            doc = new SwiftlyTextDocument(null, element, ProjectStorage.TEXT_ENCODING);
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
    public void updateDocument (ClientObject caller, int elementId, String text,
                                InvocationListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        _roomObj.postEvent(
            new DocumentUpdatedEvent(_roomObj.getOid(), caller.getOid(), elementId, text));
    }

    // from interface ProjectRoomProvider
    public void deleteDocument (ClientObject caller, int elementId, InvocationListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        _roomObj.removeFromDocuments(elementId);
    }

    // from interface ProjectRoomProvider
    public void buildProject (ClientObject caller, InvocationListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        // inform all the clients that a build is starting
        _roomObj.setBuilding(true);

        // perform a commit first. if that works, it will run the build
        doCommit(true, listener);
    }

    // from interface ProjectRoomProvider
    public void commitProject (ClientObject caller, String commitMsg, ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
    }

    // from interface ProjectRoomProvider
    public void loadDocument (ClientObject caller, final PathElement element,
                              final ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        // Load the document from the storage provider
        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit("loadDocument") {
            public boolean invoke () {
                try {
                    _doc = _storage.getDocument(element);
                } catch (ProjectStorageException pse) {
                    _error = pse;
                }
                return true;
            }

            public void handleResult () {
                if (_error == null) {
                    _roomObj.addSwiftlyDocument(_doc);
                    listener.requestProcessed();
                } else {
                    log.log(Level.WARNING, "Load document failed [pathElement=" +
                        element + "].", _error);
                    listener.requestFailed("e.load_document_failed");
                }
            }

            protected SwiftlyDocument _doc;
            protected Exception _error;
        });
    }

    // from interface ProjectRoomProvider
    public void startFileUpload (final ClientObject caller, final String fileName,
                                 final PathElement parent, final ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        // attempt to find the path element in the dobj
        final PathElement element = _roomObj.findPathElement(fileName, parent);

        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit("startFileUpload") {
            public boolean invoke () {
                try {
                    // if we're updating an existing document, handle that
                    if ((element != null)) {
                        SwiftlyDocument doc = _roomObj.getDocument(element);
                        // if the document is not yet resolved we must do so now
                        if (doc == null) {
                            doc = _storage.getDocument(element);
                        }
                        _uploadFile = new UploadFile(fileName, parent, doc);
                    } else {
                        _uploadFile = new UploadFile(fileName, parent);
                    }
                } catch (Exception e) {
                    _error = e;
                }
                return true;
            }

            public void handleResult () {
                if (_error == null) {
                    _currentUploads.put(caller.getOid(), _uploadFile);
                    listener.requestProcessed();
                } else {
                    log.log(Level.WARNING, "Start upload failed [file=" + fileName + "].", _error);
                    listener.requestFailed("e.start_upload_failed");
                }
            }

            protected UploadFile _uploadFile;
            protected Exception _error;
        });
    }

    // from interface ProjectRoomProvider
    public void uploadFile (ClientObject caller, final byte[] data)
    {
        final UploadFile uploadFile = _currentUploads.get(caller.getOid());
        if (uploadFile == null) {
            return; // no way to report failure here so we lump it
        }

        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit("uploadFile") {
            public boolean invoke () {
                uploadFile.appendData(data);
                return true;
            }
        });
    }

    // from interface ProjectRoomProvider
    public void finishFileUpload (ClientObject caller, ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        UploadFile uploadFile = _currentUploads.get(caller.getOid());
        if (uploadFile == null) {
            throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
        }

        // run this task on the serial executor as the svn actions can take a while
        MsoyServer.swiftlyMan.svnExecutor.addTask(
            new FinishFileUploadTask(uploadFile, caller, listener));
    }

    // from interface ProjectRoomProvider
    public void abortFileUpload (final ClientObject caller, final ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        final UploadFile uploadFile = _currentUploads.get(caller.getOid());
        if (uploadFile == null) {
            throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
        }

        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit("abortFileUpload") {
            public boolean invoke () {
                try {
                    // cleanup the upload file
                    uploadFile.abortUpload();

                } catch (Exception error) {
                    // we'll report this on resultReceived()
                    _error = error;
                }
                return true;
            }

            public void handleResult () {
                // the upload was aborted, so remove it from the list
                _currentUploads.remove(caller.getOid());

                if (_error == null) {
                    listener.requestProcessed();
                } else {
                    log.log(Level.WARNING,
                        "Aborting upload failed [file=" + uploadFile + "].", _error);
                    listener.requestFailed("e.abort_upload_failed");
                }
            }

            protected Exception _error;
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

    @Override // from Object
    public void finalize ()
        throws Throwable
    {
        try {
            FileUtils.deleteDirectory(_buildDir);
        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to delete build directory [dir=" + _buildDir + "].", e);
        } finally {
            super.finalize();
        }
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
        onShutdownCommit();
        MsoyServer.swiftlyMan.projectDidShutdown(this);
    }

    /**
     * Issue a request on the executor to commit this project; failure will not be reported to
     * the user, only logged on the server. For use only by the server.
     */
    protected void onShutdownCommit ()
    {
        doCommit(false, new InvocationListener() {
            public void requestFailed (String reason) {
                // nada. no result will be provided to the user.
            }
        });
    }

    /**
     * Issue a request on the executor to commit this project; failure will be reported on the
     * listener. If the commit works, it will start the build if true was passed as a parameter.
     */
    protected void doCommit (boolean shouldBuild, InvocationListener listener)
    {
        MsoyServer.swiftlyMan.svnExecutor.addTask(new CommitProjectTask(shouldBuild, listener));
    }

    /**
     * Throws an InvocationException if the supplied caller is not a collaborator on the project.
     */
    protected void checkPermissions (ClientObject caller)
        throws InvocationException
    {
        MemberObject memobj = (MemberObject)caller;
        if (_collaborators.contains(memobj.getMemberId())) {
            throw new InvocationException("e.access_denied");
        }
    }

    /** Handles a request to commit our project. */
    protected class CommitProjectTask implements SerialExecutor.ExecutorTask
    {
        public CommitProjectTask (boolean shouldBuild, InvocationListener listener)
        {
            _projectId = ((ProjectRoomConfig)_config).projectId;
            _shouldBuild = shouldBuild;
            _listener = listener;
            // take a snapshot of our documents while we're on the dobj thread
            _allDocs = _roomObj.documents.toArray(new SwiftlyDocument[_roomObj.documents.size()]);
        }

        public boolean merge (SerialExecutor.ExecutorTask other)
        {
            // we don't want more than one pending commit for a project
            if (other instanceof CommitProjectTask) {
                return _projectId == ((CommitProjectTask)other)._projectId;
            }
            return false;
        }

        public long getTimeout ()
        {
            return 60 * 1000L; // 60 seconds is all you get kid
        }

        // this is called on the executor thread and can go hog wild with the blocking
        public void executeTask ()
        {
            try {
                // commit each swiftly document in the project that has changed
                for (SwiftlyDocument doc : _allDocs) {
                    if (doc.isDirty()) {
                        _storage.putDocument(doc, "Automatic Swiftly Commit");
                        doc.commit();
                        _modDocs.add(doc);
                    }
                }

            } catch (Throwable error) {
                // we'll report this on resultReceived()
                _error = error;
            }
        }

        // this is called back on the dobj thread and must only report results
        public void resultReceived ()
        {
            // update the documents that were committed (any that got added to this list got
            // committed, even if we later failed)
            for (SwiftlyDocument doc : _modDocs) {
                _roomObj.updateDocuments(doc);
            }

            if (_error != null) {
                log.log(Level.WARNING, "Project storage commit failed.", _error);
                _listener.requestFailed("e.commit_failed_unexpected");
                return;
            }

            // if the commit worked, run the build if instructed
            if (_shouldBuild) {
                MsoyServer.swiftlyMan.buildExecutor.addTask(new BuildProjectTask(_listener));
            }
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut ()
        {
            _listener.requestFailed("e.commit_timed_out");
        }

        protected SwiftlyDocument[] _allDocs;
        protected ArrayList<SwiftlyDocument> _modDocs = new ArrayList<SwiftlyDocument>();

        protected int _projectId;
        protected boolean _shouldBuild;
        protected InvocationListener _listener;
        protected Throwable _error;
    }

    /** Handles a request to build our project. */
    protected class BuildProjectTask implements SerialExecutor.ExecutorTask
    {
        public BuildProjectTask (InvocationListener listener)
        {
            _projectId = ((ProjectRoomConfig)_config).projectId;
            _listener = listener;
        }

        public boolean merge (SerialExecutor.ExecutorTask other)
        {
            // we don't want more than one pending build for a project
            if (other instanceof BuildProjectTask) {
                return _projectId == ((BuildProjectTask)other)._projectId;
            }
            return false;
        }

        public long getTimeout ()
        {
            return 60 * 1000L; // 60 seconds is all you get kid
        }

        // this is called on the executor thread and can go hog wild with the blocking
        public void executeTask ()
        {
            try {
                // Get the local build directory
                File topBuildDir = new File(ServerConfig.serverRoot + LOCAL_BUILD_DIRECTORY);

                // Create a temporary build directory
                _buildDir = File.createTempFile(
                    "localbuilder", String.valueOf(_projectId), topBuildDir);
                _buildDir.delete();
                if (_buildDir.mkdirs() != true) {
                    // This should -never- happen, try to exit gracefully.
                    log.warning("Unable to create swiftly build directory: " + _buildDir);
                    _error = new Exception("internal error");
                }

                _result = _builder.build(_buildDir);
            } catch (Throwable error) {
                // we'll report this on resultReceived()
                _error = error;
            }
        }

        // this is called back on the dobj thread and must only report results
        public void resultReceived ()
        {
            // Inform the clients that the build is finished
            _roomObj.setBuilding(false);

            if (_error != null) {
                log.log(Level.WARNING, "Project build failed.", _error);
                _listener.requestFailed("e.build_failed_unexpected");
                return;
            }

            // Check for failure
            if (_result.buildSuccessful()) {
                // TODO: This is an awful last minute hack!
                try {
                    File endResult = File.createTempFile(
                        "buildresult" + Integer.toString(_projectId), ".swf",
                        new File(ServerConfig.serverRoot + "/pages/buildresults/"));
                    FileUtils.copyFile(_result.getOutputFile(), endResult);
                    FileUtils.deleteDirectory(_buildDir);
                    String url = "http://" + ServerConfig.serverHost + ":" +
                        ServerConfig.getHttpPort() + "/buildresults/" + endResult.getName();
                    _result.setBuildResultURL(url);
                } catch (IOException ioe) {
                    // XXX HACK HACK HACK
                }

            }

            // Provide build output
            _roomObj.setResult(_result);
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut ()
        {
            _listener.requestFailed("e.build_timed_out");
        }

        protected File _buildDir;
        protected int _projectId;
        protected InvocationListener _listener;
        protected Throwable _error;
        protected BuildResult _result;
    }

    /** Handles a request to finalize an uploaded file. */
    protected class FinishFileUploadTask implements SerialExecutor.ExecutorTask
    {
        public FinishFileUploadTask (UploadFile uploadFile, ClientObject caller,
                                     ConfirmListener listener)
        {
            _uploadFile = uploadFile;
            _caller = caller;
            _listener = listener;
        }

        public boolean merge (SerialExecutor.ExecutorTask other)
        {
            return false;
        }

        public long getTimeout ()
        {
            return 60 * 1000L; // 60 seconds is all you get kid
        }

        // this is called on the executor thread and can go hog wild with the blocking
        public void executeTask ()
        {
            try {
                // finalize the upload
                _doc = _uploadFile.finishUpload();

                // save the document to the repository
                _storage.putDocument(_doc, "Automatic Swiftly Commit");
                _doc.commit();
            } catch (Throwable error) {
                // we'll report this on resultReceived()
                _error = error;
            }
        }

        // this is called back on the dobj thread and must only report results
        public void resultReceived ()
        {
            // if we failed, stop here
            if (_error != null) {
                if (_error instanceof CannotDetermineMimeTypeException) {
                    // let's see what people were trying to upload for now
                    log.warning("Unable to determine mime type." + _error);
                    _listener.requestFailed("e.mimetype_not_found");
                } else {
                    log.log(Level.WARNING, "Finish upload failed.", _error);
                    _listener.requestFailed("e.finish_upload_failed");
                }
                return;
            }

            // otherwise add or update our path element and document
            PathElement element = _doc.getPathElement();
            if (element.elementId == 0) {
                _roomObj.addPathElement(element);
            } else {
                _roomObj.updatePathElements(element);
            }
            if (_doc.documentId == 0) {
                _roomObj.addSwiftlyDocument(_doc);
            } else {
                _roomObj.updateDocuments(_doc);
            }

            // the upload worked, so remove it from the list
            _currentUploads.remove(_caller.getOid());

            // and let the caller know we finally made it
            _listener.requestProcessed();
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut ()
        {
            _listener.requestFailed("e.file_upload_timed_out");
        }

        protected UploadFile _uploadFile;
        protected ClientObject _caller;
        protected ConfirmListener _listener;
        protected Throwable _error;
        protected SwiftlyDocument _doc;
    }

    /** Handles tracking a file upload for a user. */
    protected static class UploadFile
    {
        public UploadFile (String fileName, PathElement parent)
            throws IOException
        {
            this(fileName, parent, null);
        }

        public UploadFile (String fileName, PathElement parent, SwiftlyDocument doc)
            throws IOException
        {
            _fileName = fileName;
            _parent = parent;
            _doc = doc;

            // setup the temp file
            _tempFile = File.createTempFile("swiftlyupload", ".uploadfile");
            _tempFile.deleteOnExit();
            _fileOutput = new FileOutputStream(_tempFile);
        }

        public void appendData (byte[] data)
        {
            // if we have already encountered an IOException, don't try to append any more data
            if (_error != null) {
                return;
            }

            try {
                _fileOutput.write(data);
            } catch (IOException ioe) {
                _error = ioe;
                log.warning("Append upload data failed [file=" +  _fileName +
                            ", error=" + ioe + "].");
            }
        }

        public SwiftlyDocument finishUpload()
            throws IOException, CannotDetermineMimeTypeException
        {
            try {
                // close the FileOutputStream
                _fileOutput.close();

                // if we encountered an error during appendData, throw that now
                if (_error != null) {
                    throw _error;
                }

                // determine the mime type of the uploaded file
                SwiftlyMimeTypeIdentifier identifier = new SwiftlyMimeTypeIdentifier();
                String mimeType = identifier.determineMimeType(_fileName, _tempFile);

                // if we were unable to determine the mimetype, throw an exception
                if (mimeType == null) {
                    throw new CannotDetermineMimeTypeException("File name: " + _fileName);
                }

                // if we already have an existing file, update it
                if (_doc != null) {
                    _doc.getPathElement().setMimeType(mimeType);
                    _doc.setData(new FileInputStream(_tempFile), ProjectStorage.TEXT_ENCODING);
                    return _doc;

                } else {
                    // create the new PathElement
                    PathElement element = PathElement.createFile(_fileName, _parent, mimeType);

                    // create the new, blank SwiftlyDocument
                    SwiftlyDocument doc = SwiftlyDocument.createFromPathElement(element,
                        ProjectStorage.TEXT_ENCODING);
                    // insert the uploaded file data into the new document
                    doc.setData(new FileInputStream(_tempFile), ProjectStorage.TEXT_ENCODING);
                    return doc;
                }

            } finally {
                // remove the temp file no matter what
                _tempFile.delete();
            }
        }

        public void abortUpload()
            throws IOException
        {
            try {
                // close the FileOutputStream
                _fileOutput.close();

            } finally {
                // remove the temp file no matter what
                _tempFile.delete();
            }
        }

        protected String _fileName;
        protected PathElement _parent;
        protected SwiftlyDocument _doc;
        protected File _tempFile;
        protected FileOutputStream _fileOutput;

        /** flag to track if the upload failed at any point */
        protected IOException _error;
    }

    /** An exception for when the mime type cannot be determined for a file. */
    protected static class CannotDetermineMimeTypeException extends Exception
    {
        public CannotDetermineMimeTypeException (String message)
        {
            super(message);
        }
    }

    protected ProjectRoomObject _roomObj;
    protected ArrayIntSet _collaborators;
    protected ProjectStorage _storage;
    protected LocalProjectBuilder _builder;
    protected File _buildDir;

    /** Server-root relative path to the Whirled SDK. */
    protected static final String WHIRLED_SDK = "/data/swiftly/whirled_sdk";

    /** Server-root relative path to the Flex SDK. */
    protected static final String FLEX_SDK = "/data/swiftly/flex_sdk";

    /** Server-root relative path to the server build directory. */
    protected static final String LOCAL_BUILD_DIRECTORY = "/data/swiftly/build";

    /** A map from the client OID to the current file being uploaded. */
    protected HashIntMap<UploadFile> _currentUploads;
}
