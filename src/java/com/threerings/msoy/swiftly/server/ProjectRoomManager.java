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
    public void init (SwiftlyProject project, ProjectStorage storage)
    {
        _storage = storage;

        // Stick the project in the dobj
        _roomObj.setProject(project);

        // References to our on-disk SDKs
        File flexSdk = new File(ServerConfig.serverRoot + FLEX_SDK);
        File whirledSdk = new File(ServerConfig.serverRoot + WHIRLED_SDK);

        // Setup the builder.
        _builder = new LocalProjectBuilder(
            project, _storage, flexSdk.getAbsoluteFile(), whirledSdk.getAbsoluteFile());

        _currentUploads = new HashIntMap<UploadFile>();

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
                             final InvocationListener listener)
    {
        // TODO: check access!

        // Re-bind transient instance variables
        element.lazarus(_roomObj.pathElements);

        SwiftlyTextDocument doc = null;
        try {
            doc = new SwiftlyTextDocument(element, ProjectStorage.TEXT_ENCODING);
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
        _roomObj.postEvent(
            new DocumentUpdatedEvent(_roomObj.getOid(), caller.getOid(), elementId, text));
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
    public void commitProject (ClientObject caller, String commitMsg, ConfirmListener listener)
        throws InvocationException
    {
        // TODO: check access!

        // TODO: run the commit on the executor and post the result to the listener on success or
        // failure
        throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
    }

    // from interface ProjectRoomProvider
    public void loadDocument (ClientObject caller, final PathElement element,
                              final ConfirmListener listener)
    {
        // TODO: check access!

        // Load the document from the storage provider
        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit() {
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
    public void startFileUpload (final ClientObject caller, final PathElement element,
                                 final ConfirmListener listener)
    {
        final UploadFile uploadFile = new UploadFile(element);

        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    uploadFile.initTempFile();
                } catch (IOException e) {
                    _error = e;
                }
                return true;
            }

            public void handleResult () {
                if (_error == null) {
                    _currentUploads.put(caller.getOid(), uploadFile);
                    listener.requestProcessed();
                } else {
                    log.log(Level.WARNING, "Start upload failed [file=" + element + "].", _error);
                    listener.requestFailed("e.start_upload_failed");
                }
            }

            protected Exception _error;
        });
    }

    // from interface ProjectRoomProvider
    public void uploadFile (ClientObject caller, final byte[] data)
    {
        final UploadFile uploadFile = _currentUploads.get(caller.getOid());

        // TODO: freak out if this is not true?
        if (uploadFile == null) {
            return;
        }

        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    uploadFile.appendData(data);
                } catch (IOException e) {
                    // flag the upload file object as failed
                    uploadFile.setFailed();
                    log.warning("Append upload data failed [file=" +  uploadFile.getPathElement() +
                         ", error=" + e + "].");
                }
                return true;
            }
        });
    }

    // from interface ProjectRoomProvider
    public void finishFileUpload (final ClientObject caller, final ConfirmListener listener)
    {
        final UploadFile uploadFile = _currentUploads.get(caller.getOid());

        // TODO: freak out if this is not true?
        if (uploadFile == null) {
            return;
        }

        final PathElement element = uploadFile.getPathElement();

        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    if (uploadFile.didSucceed()) {
                        // only an exception in the following should set this to false
                        uploadFile.flush();
                        // create the SwiftlyBinaryDocument and add it and the PathElement
                        // to the dobj
                        _doc = new SwiftlyBinaryDocument(uploadFile.getFileData(), element);
                    }
                    // remove the temp file no matter what
                    uploadFile.deleteTempFile();
                } catch (IOException e) {
                    _error = e;
                }
                return true;
            }

            public void handleResult () {
                if (_doc != null) {
                    _roomObj.addSwiftlyDocument(_doc);
                    _roomObj.addPathElement(element);
                    listener.requestProcessed();
                } else {
                    if (_error != null) {
                        log.log(Level.WARNING, "Finish upload failed [file=" +
                            element + "].", _error);
                    } else {
                        log.warning("Upload file failed before finishing [file=" + element + "].");
                    }
                    listener.requestFailed("e.finish_upload_failed");
                }
                _currentUploads.remove(caller.getOid());
            }

            protected Exception _error;
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

    @Override // from Object
    public void finalize ()
        throws Throwable
    {
        try {
            FileUtils.deleteDirectory(_buildDir);
        } catch (IOException e) {
            // TODO: log this
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
        doCommit(false);
        MsoyServer.swiftlyMan.projectDidShutdown(this);
    }

    /**
     * Issue a request on the executor to commit this project; any log output should be collected,
     * then published on _roomObj.console. If the commit works, it will start the build if true was
     * passed as a parameter.
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
        public void resultReceived () {
            if (_error != null) {
                _roomObj.setConsole(
                    MessageBundle.tcompose("m.build_failed_reason", _error.getMessage()));
                return;
            }

            // Check for failure
            if (_result.buildSuccessful()) {
                _roomObj.setConsole("m.build_complete");

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

            } else {
                _roomObj.setConsole("m.build_failed");
            }

            // Provide build output
            _roomObj.setResult(_result);
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut () {
            _roomObj.setConsole("m.build_timed_out");
        }

        protected File _buildDir;
        protected int _projectId;
        protected Throwable _error;
        protected BuildResult _result;
    }

    /** Handles tracking a file upload for a user. */
    protected class UploadFile
    {
        public UploadFile (PathElement element)
        {
            _pathElement = element;
        }

        public PathElement getPathElement ()
        {
            return _pathElement;
        }

        public void deleteTempFile ()
            throws IOException
        {
            _tempFile.delete();
        }

        public void setFailed ()
        {
            _succeeded = false;
        }

        public boolean didSucceed ()
        {
            return _succeeded;
        }

        public InputStream getFileData ()
            throws IOException
        {
            return new FileInputStream(_tempFile);
        }

        public void initTempFile ()
            throws IOException
        {
            File tempFile = File.createTempFile("swiftlyupload", ".uploadfile");
            tempFile.deleteOnExit();
            _fileOutput = new FileOutputStream(tempFile);
            _tempFile = tempFile;
        }

        public void appendData (byte[] data)
            throws IOException
        {
            _fileOutput.write(data);
        }

        public void flush ()
            throws IOException
        {
            _fileOutput.flush();
        }

        protected PathElement _pathElement;
        protected File _tempFile;
        protected FileOutputStream _fileOutput;
        /** flag to track if the upload failed at any point */
        protected boolean _succeeded = true;
    }

    protected ProjectRoomObject _roomObj;
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
