//
// $Id$

package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SerialExecutor;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.FurnitureRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.PetRecord;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.data.DocumentUpdatedEvent;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomMarshaller;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;
import com.threerings.msoy.swiftly.server.build.LocalProjectBuilder;
import com.threerings.msoy.swiftly.server.persist.SwiftlyCollaboratorsRecord;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.server.GenericUploadFile;
import com.threerings.msoy.web.server.UploadFile;
import com.threerings.msoy.web.server.UploadUtil;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.server.InvocationException;

/**
 * Manages a Swiftly project room.
 */
public class ProjectRoomManager extends PlaceManager
    implements ProjectRoomProvider, SetListener
{
    /**
     * Called by the {@link SwiftlyManager} after creating this project room manager.
     */
    public void init (final SwiftlyProject project, final List<MemberName> collaborators,
                      ProjectStorage storage, final ConfirmListener listener)
    {
        _storage = storage;
        _resultItems = new HashMap<MemberName, Integer>();

        // References to our on-disk SDKs
        File flexSdk = new File(ServerConfig.serverRoot + FLEX_SDK);
        File whirledSdk = new File(ServerConfig.serverRoot + WHIRLED_SDK);

        // Setup the svn executor.
        _svnExecutor = new SerialExecutor(MsoyServer.omgr);

        // Setup the builder.
        _builder = new LocalProjectBuilder(
            project, _storage, flexSdk.getAbsoluteFile(), whirledSdk.getAbsoluteFile());

        // Load the project tree from the storage provider
        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit("loadProject") {
            @Override
            public boolean invoke () {
                try {
                    // Okay, so it's not really a tree, but hey ...
                    // Load the file list from the provided storage instance.
                    _projectTree = _storage.getProjectTree();

                } catch (ProjectStorageException pse) {
                    _error = pse;
                }

                return true;
            }

            @Override
            public void handleResult () {
                if (_error != null) {
                    log.log(Level.WARNING,
                        "Loading project tree failed. [project=" + project + "].", _error);
                    listener.requestFailed(SwiftlyCodes.E_INTERNAL_ERROR);
                    shutdown();
                    return;
                }

                for (PathElement element : _projectTree) {
                    _roomObj.addPathElement(element);
                }
                // Inform any listeners that the project has been loaded by adding it to the dobj
                _roomObj.setProject(project);

                // set the list of collaborators in the dobj as well
                _roomObj.setCollaborators(new DSet<MemberName>(collaborators));

                // the project and collaborators have changed, send out an access control change
                _roomObj.postMessage(ProjectRoomObject.ACCESS_CONTROL_CHANGE);

                listener.requestProcessed();
            }

            protected List<PathElement> _projectTree;
            protected Exception _error;
        });
    }

    // from interface ProjectRoomProvider
    public void addPathElement (ClientObject caller, PathElement element,
                                InvocationListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);

        // for now just update the room object
        _roomObj.addPathElement(element);
    }

    // from interface ProjectRoomProvider
    public void updatePathElement (ClientObject caller, PathElement element,
                                   InvocationListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);

        _roomObj.updatePathElements(element);
    }

    // from interface ProjectRoomProvider
    public void deletePathElement (ClientObject caller, final int elementId,
                                   final ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);

        final PathElement element = _roomObj.pathElements.get(elementId);
        if (element == null) {
            throw new InvocationException(SwiftlyCodes.E_INTERNAL_ERROR);
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
            @Override
            public boolean invoke () {
                try {
                    _storage.deleteDocument(element, "Automatic Swiftly Delete");
                } catch (ProjectStorageException pse) {
                    _error = pse;
                }
                return true;
            }

            @Override
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
        requireWritePermissions(caller);

        final PathElement element = _roomObj.pathElements.get(elementId);
        if (element == null) {
            throw new InvocationException(SwiftlyCodes.E_INTERNAL_ERROR);
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
            @Override
            public boolean invoke () {
                try {
                    _storage.renameDocument(element, newName, "Automatic Swiftly Rename");
                } catch (ProjectStorageException pse) {
                    _error = pse;
                }
                return true;
            }

            @Override
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
        requireWritePermissions(caller);

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
        requireWritePermissions(caller);

        _roomObj.postEvent(
            new DocumentUpdatedEvent(_roomObj.getOid(), caller.getOid(), elementId, text));
    }

    // from interface ProjectRoomProvider
    public void deleteDocument (ClientObject caller, int elementId, InvocationListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);

        _roomObj.removeFromDocuments(elementId);
    }

    // from interface ProjectRoomProvider
    public void buildProject (ClientObject caller, ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);

        // inform all the clients that a build is starting
        _roomObj.setBuilding(true);

        BuildProjectTask buildTask = new BuildProjectTask(_roomObj.project, listener);
        _svnExecutor.addTask(new CommitProjectTask(buildTask, listener));
    }

    // from interface ProjectRoomProvider
    public void buildAndExportProject (ClientObject caller, ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);
        MemberObject memobj = (MemberObject)caller;

        // inform all the clients that a build is starting
        _roomObj.setBuilding(true);

        ExportData exportData = null;
        // look up the build result id if we have already resolved it
        Integer resultId = _resultItems.get(memobj.memberName);
        if (resultId == null) {
            exportData = new ExportData(_roomObj.project, memobj.memberName);
        } else {
            exportData = new ExportData(_roomObj.project, memobj.memberName, resultId.intValue());
        }

        BuildProjectTask buildTask = new BuildProjectTask(_roomObj.project, exportData, listener);
        _svnExecutor.addTask(new CommitProjectTask(buildTask, listener));
    }

    // from interface ProjectRoomProvider
    public void loadDocument (ClientObject caller, final PathElement element,
                              final ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireReadPermissions(caller);

        // Load the document from the storage provider
        MsoyServer.swiftlyInvoker.postUnit(new Invoker.Unit("loadDocument") {
            @Override
            public boolean invoke () {
                try {
                    _doc = _storage.getDocument(element);
                } catch (ProjectStorageException pse) {
                    _error = pse;
                }
                return true;
            }

            @Override
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

    /**
     * Used by the SwiftlyUploadServlet to transfer the upload file data into the room. Permission
     * to perform this action should be checked by the caller.
     */
    public void insertUploadFile (UploadFile uploadFile, ResultListener<Void> listener)
    {
        // if we're updating an existing document, handle that
        PathElement element = _roomObj.findPathElement(
            uploadFile.getOriginalName(), _roomObj.getRootElement());
        if (element != null) {
            // let's try to pull the resolved document from the room object. this may return null
            // in which case the InsertFileUploadTask will load it from the repository
            SwiftlyDocument doc = _roomObj.getDocument(element);
            _svnExecutor.addTask(new InsertFileUploadTask(uploadFile, element, doc, listener));

        // otherwise this is a new file
        } else {
            _svnExecutor.addTask(new InsertFileUploadTask(uploadFile, listener));
        }
    }

    /**
     * Used by the SwiftlyServlet to update the local list of collaborators.
     */
    public void addCollaborator (MemberName name, ResultListener<Void> lner)
    {
        _roomObj.addToCollaborators(name);
        _roomObj.postMessage(ProjectRoomObject.ACCESS_CONTROL_CHANGE);
        lner.requestCompleted(null);
    }

    /**
     * Used by the SwiftlyServlet to update the local list of collaborators.
     */
    public void removeCollaborator (MemberName name, ResultListener<Void> lner)
    {
        _roomObj.removeFromCollaborators(name.getKey());
        _roomObj.postMessage(ProjectRoomObject.ACCESS_CONTROL_CHANGE);
        _resultItems.remove(name);
        lner.requestCompleted(null);
    }

    /**
     * Used by the SwiftlyServlet to update the room object project object.
     */
    public void updateProject (ResultListener<Void> lner, final SwiftlyProject project)
    {
        _roomObj.setProject(project);
        _roomObj.postMessage(ProjectRoomObject.ACCESS_CONTROL_CHANGE);
        lner.requestCompleted(null);
    }

    /**
     * Throws an InvocationException if the caller should not be able to modify the project.
     */
    public void requireWritePermissions (ClientObject caller)
        throws InvocationException
    {
        MemberObject memobj = (MemberObject)caller;
        if (!_roomObj.hasWriteAccess(memobj.memberName)) {
            throw new InvocationException(SwiftlyCodes.E_ACCESS_DENIED);
        }
    }

    /**
     * Throws an InvocationException if the caller should not be able to view the project.
     */
    public void requireReadPermissions (ClientObject caller)
        throws InvocationException
    {
        MemberObject memobj = (MemberObject)caller;
        if (!_roomObj.hasReadAccess(memobj.memberName)) {
            throw new InvocationException(SwiftlyCodes.E_ACCESS_DENIED);
        }
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

        // TODO: wait for our svn serial executor to finish, but timeout if it takes more than 10
        // seconds or so

        MsoyServer.swiftlyMan.projectDidShutdown(this);
    }

    /**
     * Issue a request on the executor to commit this project; failure will not be reported to
     * the user, only logged on the server. For use only by the server.
     */
    protected void onShutdownCommit ()
    {
        ConfirmListener listener = new ConfirmListener() {
            public void requestProcessed ()
            {
                // nada. no result will be provided to the user.
            }
            public void requestFailed (String reason) {
                // nada. no result will be provided to the user.
            }

        };
        _svnExecutor.addTask(new CommitProjectTask(listener));
    }

    /** Handles a request to commit our project. */
    protected class CommitProjectTask implements SerialExecutor.ExecutorTask
    {
        /**
         * Only commit the project, do not perform a build.
         */
        public CommitProjectTask (ConfirmListener listener)
        {
            this(null, listener);
        }

        /**
         * Commit the project, then perform a build.
         */
        public CommitProjectTask (BuildProjectTask buildTask, ConfirmListener listener)
        {
            _buildTask = buildTask;
            _listener = listener;
            // take a snapshot of certain items while we're on the dobj thread
            _allDocs = _roomObj.documents.toArray(new SwiftlyDocument[_roomObj.documents.size()]);
        }

        public boolean merge (SerialExecutor.ExecutorTask other)
        {
            return true;
        }

        public long getTimeout ()
        {
            return 60 * 1000L; // 60 seconds is all you get kid
        }

        // this is called on the executor thread and can go hog wild with the blocking
        public void executeTask ()
        {
            _startTime = System.currentTimeMillis();
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
            if (buildRequested()) {
                _buildTask.setStartTime(_startTime);
                MsoyServer.swiftlyMan.buildExecutor.execute(_buildTask);

            } else {
                _listener.requestProcessed();
            }
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut ()
        {
            _listener.requestFailed("e.commit_timed_out");
        }

        protected boolean buildRequested ()
        {
            return (_buildTask != null);
        }

        protected final SwiftlyDocument[] _allDocs;
        protected final ArrayList<SwiftlyDocument> _modDocs = new ArrayList<SwiftlyDocument>();

        protected final BuildProjectTask _buildTask;
        protected final ConfirmListener _listener;
        protected Throwable _error;
        protected long _startTime;
    }

    /** Handles a request to build our project. */
    protected class BuildProjectTask implements Runnable
    {
        public BuildProjectTask (SwiftlyProject project, ConfirmListener listener)
        {
            this(project, null, listener);
        }

        public BuildProjectTask (SwiftlyProject project, ExportData exportData,
                                 ConfirmListener listener)
        {
            _project = project;
            _exportData = exportData;
            _listener = listener;
        }

        // from Runnable
        public void run ()
        {
            try {
                // Get the local build directory
                File topBuildDir = new File(ServerConfig.serverRoot + LOCAL_BUILD_DIRECTORY);

                // Create a temporary build directory
                _buildDir = File.createTempFile("localbuilder", String.valueOf(_project.projectId),
                    topBuildDir);
                _buildDir.delete();
                if (_buildDir.mkdirs() != true) {
                    // This should -never- happen, try to exit gracefully.
                    log.warning("Unable to create swiftly build directory: " + _buildDir);
                    _error = new Exception("internal error");
                }

                // build the project
                _result = _builder.build(_buildDir);

                // Only publish the result if the build succeeded and the caller asked
                if (_result.buildSuccessful() && exportResult()) {
                    publishResult();
                }

            } catch (Throwable error) {
                // we'll report this on resultReceived()
                _error = error;

            } finally {
                // finally clean up the build results.
                try {
                    if (_buildDir != null) {
                        FileUtils.deleteDirectory(_buildDir);
                    }
                } catch (IOException ioe) {
                    // only log to the server if this fails, client doesn't care.
                    log.log(Level.WARNING,
                        "Failed to delete temporary build results directory.", ioe);
                }

                // deal with post processing the build on the dobject thread
                MsoyServer.omgr.postRunnable(new Runnable() {
                    public void run() {
                        resultReceived();
                    }
                });
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

            if (_result.buildSuccessful() && exportResult()) {
                // inform the item manager of the new or updated item
                if (_record.itemId == 0) {
                    MsoyServer.itemMan.itemCreated(_record);
                } else {
                    MsoyServer.itemMan.itemUpdated(_record);
                }
                // update the build result id cache
                _resultItems.put(_exportData.member, _exportData.buildResultItemId);
            }

            // set the full time of the task [including commit] in the result
            _result.setBuildTime(System.currentTimeMillis() - _startTime);

            // Provide build output to the room
            _roomObj.setResult(_result);

            // inform the listener that the build service call worked. the caller will need to
            // check if the build succeeded using the room object.
            _listener.requestProcessed();
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut ()
        {
            _listener.requestFailed("e.build_timed_out");
        }

        public void setStartTime(long time)
        {
            _startTime = time;
        }

        /**
         * Returns true if the build result should be exported.
         */
        protected boolean exportResult ()
        {
            return (_exportData != null);
        }

        /**
         * Publish the build results into the media store.
         * @throws Exception
         */
        protected void publishResult ()
            throws Exception
        {
            // First, publish the results into the media store
            UploadFile uploadFile = new GenericUploadFile(_result.getOutputFile());
            UploadUtil.publishUploadFile(uploadFile);

            // load the correct item repository
            ItemRepository<ItemRecord, ?, ?, ?> repo =
                MsoyServer.itemMan.getRepository(_exportData.itemType());

            // if the build result id was not in the room cache, load the record
            if (_exportData.noBuildResult()) {
                SwiftlyCollaboratorsRecord sRec = MsoyServer.swiftlyRepo.loadCollaborator(
                    _exportData.projectId, _exportData.memberId);
                if (sRec == null) {
                    throw new PersistenceException("No collaborator record found when expected. " +
                        "[projectId=" + _exportData.projectId + ", memberId=" +
                        _exportData.memberId + "].");
                }
                _exportData.buildResultItemId = sRec.buildResultItemId;
            }

            // if the user already has an item, look it up
            if (_exportData.buildResultItemId > 0) {
                _record = repo.loadItem(_exportData.buildResultItemId);
            }

            // if the item was null [meaning they never had one or it was deleted] or
            // the user is no longer the owner, then create a new item.
            if (_record == null || _record.ownerId != _exportData.memberId) {
                Item item = null;
                // can't use switch since Item.* are not constants
                if (_exportData.itemType() == Item.AVATAR) {
                    Avatar avatar = new Avatar();
                    avatar.avatarMedia = new MediaDesc(
                        MediaDesc.stringToHash(uploadFile.getHash()), uploadFile.getMimeType());
                    item = avatar;

                } else if (_exportData.itemType() == Item.GAME) {
                    Game game = new Game();
                    game.gameMedia = new MediaDesc(
                        MediaDesc.stringToHash(uploadFile.getHash()), uploadFile.getMimeType());
                    // game.config cannot be null so just set it to blank and the user can
                    // tweak the config settings through the item editor
                    game.config = "";
                    item = game;

                } else if (_exportData.itemType() == Item.FURNITURE) {
                    Furniture furniture = new Furniture();
                    furniture.furniMedia = new MediaDesc(
                        MediaDesc.stringToHash(uploadFile.getHash()), uploadFile.getMimeType());
                    item = furniture;

                } else if (_exportData.itemType() == Item.PET) {
                    Pet pet = new Pet();
                    pet.furniMedia = new MediaDesc(
                        MediaDesc.stringToHash(uploadFile.getHash()), uploadFile.getMimeType());
                    item = pet;

                } else {
                    throw new Exception(
                        "Unsupported itemType encountered during Swiftly item exporting.");
                }

                // setup the rest of the generic item fields
                // TODO: Figure out a way to i18n this string
                item.name = _exportData.projectName + " Swiftly Result";
                // description cannot be NULL
                item.description = "";
                item.ownerId = _exportData.memberId;
                item.creatorId = _exportData.memberId;
                _record = ItemRecord.newRecord(item);

                // insert the new item into the repository
                repo.insertOriginalItem(_record);

                // update the collaborator record with the new itemId
                _exportData.buildResultItemId = _record.itemId;
                MsoyServer.swiftlyRepo.updateBuildResultItem(
                    _exportData.projectId, _exportData.memberId, _record.itemId);

            // otherwise, update the existing item
            } else {
                // can't use switch since Item.* are not constants
                ItemRecord updateRecord = null;
                if (_exportData.itemType() == Item.AVATAR) {
                    AvatarRecord avatarRecord = (AvatarRecord) _record;
                    avatarRecord.avatarMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                    updateRecord = avatarRecord;

                } else if (_exportData.itemType() == Item.GAME) {
                    GameRecord gameRecord = (GameRecord) _record;
                    gameRecord.gameMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                    updateRecord = gameRecord;

                } else if (_exportData.itemType() == Item.FURNITURE) {
                    FurnitureRecord furnitureRecord = (FurnitureRecord) _record;
                    furnitureRecord.furniMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                    updateRecord = furnitureRecord;

                } else if (_exportData.itemType() == Item.PET) {
                    PetRecord petRecord = (PetRecord) _record;
                    petRecord.furniMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                    updateRecord = petRecord;

                } else {
                    throw new Exception(
                        "Unsupported itemType encountered during Swiftly item exporting.");
                }

                repo.updateOriginalItem(updateRecord);
            }
        }

        protected final SwiftlyProject _project;
        protected final ExportData _exportData;
        protected final ConfirmListener _listener;
        protected BuildResult _result;
        protected ItemRecord _record;
        protected File _buildDir;
        protected Throwable _error;
        protected long _startTime;
    }

    /** Handles inserting the upload file data into svn and the room object. */
    protected class InsertFileUploadTask implements SerialExecutor.ExecutorTask
    {
        public InsertFileUploadTask (UploadFile uploadFile, ResultListener<Void> listener)
        {
            this(uploadFile, null, null, listener);
        }

        public InsertFileUploadTask (UploadFile uploadFile, PathElement element,
                                     SwiftlyDocument doc, ResultListener<Void> listener)
        {
            _uploadFile = uploadFile;
            _element = element;
            _doc = doc;
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
                // if we were given a valid path element, but the document was not resolved in
                // the room object, we need to load it from the storage repository
                if (_element != null && _doc == null) {
                    _doc = _storage.getDocument(_element);

                // else if we have no path element, then this is a new document. the final "else"
                // is that we have an existing path element and we already found the document
                // so just proceed.
                } else if (_element == null) {
                    // TODO: as we currently have no way of getting from GWT which "directory"
                    // the user wants this element to go into, we are going to assume that the root
                    // is the parent of the new element.
                    _element = PathElement.createFile(_uploadFile.getOriginalName(),
                        _roomObj.getRootElement(), _uploadFile.getMimeTypeAsString());

                    // create the new, blank SwiftlyDocument
                    _doc = SwiftlyDocument.createFromPathElement(_element,
                        ProjectStorage.TEXT_ENCODING);
                }

               // insert the uploaded file data into the new document
               _doc.setData(_uploadFile.getInputStream(), ProjectStorage.TEXT_ENCODING);

               // save the document to the repository
               _storage.putDocument(_doc, "Automatic Swiftly Upload Commit");
               _doc.commit();

            } catch (Exception error) {
                // we'll report this on resultReceived()
                _error = error;
            }
        }

        // meanwhile, back on the dobject thread
        public void resultReceived ()
        {
            if (_error != null) {
                log.log(Level.WARNING, "insertUploadFile failed [element=" + _element + "].",
                    _error);
                _listener.requestFailed(_error);
                return;
            }

            // update or add the document and element in the room object
            if (_element.elementId == 0) {
                _roomObj.addPathElement(_element);
            } else {
                _roomObj.updatePathElements(_element);
            }
            if (_doc.documentId == 0) {
                _roomObj.addSwiftlyDocument(_doc);
            } else {
                _roomObj.updateDocuments(_doc);
            }

            // inform the listener everything worked
            _listener.requestCompleted(null);
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut ()
        {
            _listener.requestFailed(new Exception("InsertFileUploadTask timed out."));
        }

        protected final UploadFile _uploadFile;
        protected PathElement _element;
        protected SwiftlyDocument _doc;
        protected final ResultListener<Void> _listener;
        protected Exception _error;
    }

    /**
     * Small data class to hold dobject thread objects needed during the build export process.
     */
    protected static class ExportData
    {

        public final int projectId;
        public final int memberId;
        public final MemberName member;
        public final String projectName;
        public final int projectType;
        public int buildResultItemId;

        // the build result item id has not yet been resolved into the room cache
        public ExportData (SwiftlyProject project, MemberName member)
        {
            this(project, member, RECORD_NOT_LOADED);
        }
        // the build result item id has been resolved into the room cache
        public ExportData (SwiftlyProject project, MemberName member, int buildResultItemId)
        {
            // since GWT does not support clone, we'll pull off the primitives we want from
            // the project object, which came from the dobject thread.
            this.projectId = project.projectId;
            this.projectName = project.projectName;
            this.projectType = project.projectType;

            this.memberId = member.getMemberId();
            this.member = member;
            this.buildResultItemId = buildResultItemId;
        }

        /**
         * The item type being built by this project.
         */
        public byte itemType ()
        {
            return (byte)this.projectType;
        }

        /**
         * Returns true if the buildResultItemId has not yet been resolved from the database.
         */
        public boolean noBuildResult ()
        {
            return (this.buildResultItemId == RECORD_NOT_LOADED);
        }

        /** indicates that the collaborator record has not been looked up yet */
        protected static final int RECORD_NOT_LOADED = -1;
    }

    /** Server-root relative path to the Whirled SDK. */
    protected static final String WHIRLED_SDK = "/data/swiftly/whirled_sdk";

    /** Server-root relative path to the Flex SDK. */
    protected static final String FLEX_SDK = "/data/swiftly/flex_sdk";

    /** Server-root relative path to the server build directory. */
    protected static final String LOCAL_BUILD_DIRECTORY = "/data/swiftly/build";

    /** This is used to execute potentially long running svn operations serially on a separate
     * thread so that they do not interfere with normal server operation. */
    protected SerialExecutor _svnExecutor;

    /** Cache the memberId to build result itemId mapping used for exporting results */
    protected Map<MemberName, Integer> _resultItems;

    protected ProjectRoomObject _roomObj;
    protected ProjectStorage _storage;
    protected LocalProjectBuilder _builder;
    protected File _buildDir;

}
