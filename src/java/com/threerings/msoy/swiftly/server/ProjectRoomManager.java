//
// $Id$

package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SerialExecutor;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.msoy.data.MemberObject;
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
import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
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
    public void init (final SwiftlyProject project,
                      HashIntMap<SwiftlyCollaboratorsRecord> collaborators, ProjectStorage storage)
    {
        _storage = storage;
        _collaborators = collaborators;

        // References to our on-disk SDKs
        File flexSdk = new File(ServerConfig.serverRoot + FLEX_SDK);
        File whirledSdk = new File(ServerConfig.serverRoot + WHIRLED_SDK);

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
                    return true;
                } catch (ProjectStorageException pse) {
                    // TODO: Handle this how?
                    log.log(Level.WARNING,
                        "Loading project tree failed. [project=" + project + "].", pse);
                    return false;
                }
            }

            @Override
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
    public void buildProject (ClientObject caller, ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);

        // perform a commit first. if that works, it will run the build
        doCommitAndBuild(listener);
    }

    // from interface ProjectRoomProvider
    public void buildAndExportProject (ClientObject caller, ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        checkPermissions(caller);
        MemberObject memobj = (MemberObject)caller;

        // grab the collaborator record on the dobject thread
        SwiftlyCollaboratorsRecord record = _collaborators.get(memobj.getMemberId());

        // perform a commit first. if that works, it will run the build and export the result
        doCommitAndBuildAndExport(listener, record);
    }

    // from interface ProjectRoomProvider
    public void loadDocument (ClientObject caller, final PathElement element,
                              final ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        // TODO: this only protects the initial loading of the document. Once loaded into the dset
        // any user in the room can load the file from the dset. For read only mode we're going
        // to want to allow anyone to load a document anyway so this is going to have to change.
        checkPermissions(caller);

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
            MsoyServer.swiftlyMan.svnExecutor.addTask(
                new InsertFileUploadTask(uploadFile, element, doc, listener));

        // otherwise this is a new file
        } else {
            MsoyServer.swiftlyMan.svnExecutor.addTask(
                new InsertFileUploadTask(uploadFile, listener));
        }
    }

    /**
     * Used by the SwiftlyServlet to update the local list of collaborators.
     */
    public void updateCollaborators (ResultListener<Void> lner)
    {
        final int projectId = _roomObj.project.projectId;
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>("getCollaborators", lner) {
            @Override
            public Void invokePersistResult () throws PersistenceException {
                _newCollaborators = new HashIntMap<SwiftlyCollaboratorsRecord>();
                for (SwiftlyCollaboratorsRecord record :
                    MsoyServer.swiftlyRepo.getCollaborators(projectId)) {
                    _newCollaborators.put(record.memberId, record);
                }
                return null;
            }

            @Override
            public void handleResult () {
                _collaborators = _newCollaborators;
                _listener.requestCompleted(null);
            }

            protected HashIntMap<SwiftlyCollaboratorsRecord> _newCollaborators;
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
        doCommitOnly(new ConfirmListener() {
            public void requestProcessed ()
            {
                // nada. no result will be provided to the user.
            }
            public void requestFailed (String reason) {
                // nada. no result will be provided to the user.
            }

        });
    }

    /**
     * Just commit the project, no build.
     */
    protected void doCommitOnly (ConfirmListener listener)
    {
        MsoyServer.swiftlyMan.svnExecutor.addTask(new CommitProjectTask(listener));
    }

    /**
     * Commit the project and then build.
     */
    protected void doCommitAndBuild (ConfirmListener listener)
    {
        // inform all the clients that a build is starting
        _roomObj.setBuilding(true);

        MsoyServer.swiftlyMan.svnExecutor.addTask(
            new CommitProjectTask(new BuildData(_roomObj.project), listener));
    }

    /**
     * Commit the project and then build and export the result.
     */
    protected void doCommitAndBuildAndExport (ConfirmListener listener,
                                              SwiftlyCollaboratorsRecord collabRecord)
    {
        // inform all the clients that a build is starting
        _roomObj.setBuilding(true);

        MsoyServer.swiftlyMan.svnExecutor.addTask(
            new CommitProjectTask(new BuildData(_roomObj.project, collabRecord), listener));
    }

    /**
     * Throws an InvocationException if the supplied caller is not a collaborator on the project.
     */
    protected void checkPermissions (ClientObject caller)
        throws InvocationException
    {
        MemberObject memobj = (MemberObject)caller;
        if (!_collaborators.containsKey(memobj.getMemberId())) {
            throw new InvocationException("e.access_denied");
        }
    }

    /** Handles a request to commit our project. */
    protected class CommitProjectTask implements SerialExecutor.ExecutorTask
    {
        public final int projectId;

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
        public CommitProjectTask (BuildData buildData, ConfirmListener listener)
        {
            _buildData = buildData;
            _listener = listener;
            // take a snapshot of certain items while we're on the dobj thread
            this.projectId = ((ProjectRoomConfig)_config).projectId;
            _allDocs = _roomObj.documents.toArray(new SwiftlyDocument[_roomObj.documents.size()]);
        }

        public boolean merge (SerialExecutor.ExecutorTask other)
        {
            // we don't want more than one pending commit for a project
            if (other instanceof CommitProjectTask) {
                return this.projectId == ((CommitProjectTask)other).projectId;
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
            if (buildRequested()) {
                MsoyServer.swiftlyMan.buildExecutor.addTask(
                    new BuildProjectTask(_buildData, _listener));
            }
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut ()
        {
            _listener.requestFailed("e.commit_timed_out");
        }

        protected boolean buildRequested ()
        {
            return (_buildData != null);
        }

        protected SwiftlyDocument[] _allDocs;
        protected ArrayList<SwiftlyDocument> _modDocs = new ArrayList<SwiftlyDocument>();

        protected BuildData _buildData;
        protected ConfirmListener _listener;
        protected Throwable _error;
    }

    /** Handles a request to build our project. */
    protected class BuildProjectTask implements SerialExecutor.ExecutorTask
    {
        public final int projectId;

        public BuildProjectTask (BuildData buildData, ConfirmListener listener)
        {
            _buildData = buildData;
            _listener = listener;
            this.projectId = buildData.projectId;
        }

        public boolean merge (SerialExecutor.ExecutorTask other)
        {
            // we don't want more than one pending build for a project
            if (other instanceof BuildProjectTask) {
                return this.projectId == ((BuildProjectTask)other).projectId;
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
                    "localbuilder", String.valueOf(_buildData.projectId), topBuildDir);
                _buildDir.delete();
                if (_buildDir.mkdirs() != true) {
                    // This should -never- happen, try to exit gracefully.
                    log.warning("Unable to create swiftly build directory: " + _buildDir);
                    _error = new Exception("internal error");
                }

                // build the project
                _result = _builder.build(_buildDir);

                // Only publish the result if the build succeeded and the caller asked
                if (_result.buildSuccessful() && _buildData.exportResults()) {
                    publishResult();
                }

            } catch (Throwable error) {
                // we'll report this on resultReceived()
                _error = error;

            } finally {
                // finally clean up the build results.
                try {
                    FileUtils.deleteDirectory(_buildDir);
                } catch (IOException ioe) {
                    // only log to the server if this fails, client doesn't care.
                    log.log(Level.WARNING,
                        "Failed to delete temporary build results directory.", ioe);
                }
            }
        }

        protected void publishResult ()
            throws Exception
        {
            // First, publish the results into the media store
            UploadFile uploadFile = new GenericUploadFile(_result.getOutputFile());
            UploadUtil.publishUploadFile(uploadFile);

            // load the correct item repository
            ItemRepository<ItemRecord, ?, ?, ?> repo =
                MsoyServer.itemMan.getRepository(_buildData.itemType());

            // if the user already has an item, look it up
            if (_buildData.record.buildResultItemId > 0) {
                _record = repo.loadItem(_buildData.record.buildResultItemId);
            }

            // if the item was null [meaning they never had one or it was deleted] or
            // the user is no longer the owner, then create a new item.
            if (_record == null || _record.ownerId != _buildData.record.memberId) {
                Item item = null;
                // can't use switch since Item.* are not constants
                if (_buildData.itemType() == Item.AVATAR) {
                    Avatar avatar = new Avatar();
                    avatar.avatarMedia = new MediaDesc(
                        MediaDesc.stringToHash(uploadFile.getHash()), uploadFile.getMimeType());
                    item = avatar;

                } else if (_buildData.itemType() == Item.GAME) {
                    Game game = new Game();
                    game.gameMedia = new MediaDesc(
                        MediaDesc.stringToHash(uploadFile.getHash()), uploadFile.getMimeType());
                    item = game;

                } else if (_buildData.itemType() == Item.FURNITURE) {
                    Furniture furniture = new Furniture();
                    furniture.furniMedia = new MediaDesc(
                        MediaDesc.stringToHash(uploadFile.getHash()), uploadFile.getMimeType());
                    item = furniture;

                } else if (_buildData.itemType() == Item.PET) {
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
                item.name = _buildData.projectName + " Swiftly Result";
                // description cannot be NULL
                item.description = "";
                item.ownerId = _buildData.record.memberId;
                item.creatorId = _buildData.record.memberId;
                _record = ItemRecord.newRecord(item);

                // insert the new item into the repository
                repo.insertOriginalItem(_record);

                // update the collaborator record with the new itemId
                _buildData.record.buildResultItemId = _record.itemId;
                MsoyServer.swiftlyRepo.updateBuildResultItem(_buildData.record);

            // otherwise, update the existing item
            } else {
                // can't use switch since Item.* are not constants
                ItemRecord updateRecord = null;
                if (_buildData.itemType() == Item.AVATAR) {
                    AvatarRecord avatarRecord = (AvatarRecord) _record;
                    avatarRecord.avatarMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                    updateRecord = avatarRecord;

                } else if (_buildData.itemType() == Item.GAME) {
                    GameRecord gameRecord = (GameRecord) _record;
                    gameRecord.gameMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                    updateRecord = gameRecord;

                } else if (_buildData.itemType() == Item.FURNITURE) {
                    FurnitureRecord furnitureRecord = (FurnitureRecord) _record;
                    furnitureRecord.furniMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                    updateRecord = furnitureRecord;

                } else if (_buildData.itemType() == Item.PET) {
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

            // Provide build output
            _roomObj.setResult(_result);

            if (_result.buildSuccessful() && _buildData.exportResults()) {
                // inform the item manager of the new or updated item
                if (_record.itemId == 0) {
                    MsoyServer.itemMan.itemCreated(_record);
                } else {
                    MsoyServer.itemMan.itemUpdated(_record);
                }

                // inform the listener that the result was exported.
                _listener.requestProcessed();
            }
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut ()
        {
            _listener.requestFailed("e.build_timed_out");
        }

        protected BuildData _buildData;
        protected ConfirmListener _listener;
        protected ItemRecord _record;
        protected File _buildDir;
        protected Throwable _error;
        protected BuildResult _result;
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
     * Small data class to hold dobject thread objects needed during the build process.
     */
    protected static class BuildData
    {
        public final int projectId;
        public final String projectName;
        public final int projectType;
        public final SwiftlyCollaboratorsRecord record;

        // only store information needed for building
        public BuildData (SwiftlyProject project)
        {
            this(project, null);
        }

        // store information needed for building and exporting the result
        public BuildData (SwiftlyProject project, SwiftlyCollaboratorsRecord record)
        {
            // since GWT does not support clone, we'll pull off the primitives we want from
            // the project object, which came from the dobject thread.
            this.projectId = project.projectId;
            this.projectName = project.projectName;
            this.projectType = project.projectType;

            // we can't call clone here it looks like or else we will get a new row in the database
            this.record = record;
        }

        /**
         * The item type being built by this project.
         */
        public byte itemType ()
        {
            return (byte)this.projectType;
        }

        /**
         * Whether this instance of BuildData should have its results exported.
         */
        public boolean exportResults ()
        {
            return (this.record != null);
        }
    }

    /** Server-root relative path to the Whirled SDK. */
    protected static final String WHIRLED_SDK = "/data/swiftly/whirled_sdk";

    /** Server-root relative path to the Flex SDK. */
    protected static final String FLEX_SDK = "/data/swiftly/flex_sdk";

    /** Server-root relative path to the server build directory. */
    protected static final String LOCAL_BUILD_DIRECTORY = "/data/swiftly/build";

    protected ProjectRoomObject _roomObj;
    protected HashIntMap<SwiftlyCollaboratorsRecord> _collaborators;
    protected ProjectStorage _storage;
    protected LocalProjectBuilder _builder;
    protected File _buildDir;

}
