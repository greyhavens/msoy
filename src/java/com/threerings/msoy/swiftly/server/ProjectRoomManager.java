//
// $Id$

package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Maps;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SerialExecutor;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.swiftly.data.DocumentUpdatedEvent;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomMarshaller;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;
import com.threerings.msoy.swiftly.server.build.LocalProjectBuilder;
import com.threerings.msoy.swiftly.server.build.ProjectBuilder;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.server.ServletWaiter;
import com.threerings.msoy.web.server.UploadFile;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService.ConfirmListener;
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
    /** Server-root relative path to the server build directory. */
    public static final String LOCAL_BUILD_DIRECTORY = "/data/swiftly/build";

    /**
     * Called by the {@link SwiftlyManager} after creating this project room manager.
     */
    public void init (final SwiftlyProject project, final List<MemberName> collaborators,
                      ProjectStorage storage, final ConnectConfig config,
                      final ServletWaiter<ConnectConfig> listener)
    {
        _storage = storage;
        _resultItems = Maps.newHashMap();

        // References to our on-disk SDKs
        File flexSdk = new File(ServerConfig.serverRoot + FLEX_SDK);
        File whirledSdk = new File(ServerConfig.serverRoot + WHIRLED_SDK);

        // Setup the svn executor.
        _svnExecutor = new SerialExecutor(MsoyServer.omgr);

        // Setup the builder.
        _builder = new LocalProjectBuilder(
            project, _storage, flexSdk.getAbsoluteFile(), whirledSdk.getAbsoluteFile(),
            ServerConfig.serverRoot.getAbsoluteFile());

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
                    listener.requestFailed(_error);
                    shutdown();
                    return;
                }

                for (PathElement element : _projectTree) {
                    getRoomObj().addPathElement(element);
                }
                // Inform any listeners that the project has been loaded by adding it to the dobj
                getRoomObj().setProject(project);

                // set the list of collaborators in the dobj as well
                getRoomObj().setCollaborators(new DSet<MemberName>(collaborators));

                // the project and collaborators have changed, send out an access control change
                getRoomObj().postMessage(ProjectRoomObject.ACCESS_CONTROL_CHANGE);

                // add this node to the peer manager as the project host
                MsoyServer.peerMan.projectDidStartup(project, config);

                listener.requestCompleted(config);
            }

            protected List<PathElement> _projectTree;
            protected Exception _error;
        });
    }

    // from interface ProjectRoomProvider
    public void deletePathElement (ClientObject caller, final int elementId,
                                   final ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);

        final PathElement element = getRoomObj().pathElements.get(elementId);
        if (element == null) {
            throw new InvocationException(SwiftlyCodes.E_INTERNAL_ERROR);
        }

        // if the document associated with this element is resolved, unload it (if the removal
        // fails, this will only be a minor inconvenience)
        SwiftlyDocument doc = getRoomObj().getDocument(element);
        if (doc != null) {
            getRoomObj().removeFromDocuments(doc.getKey());
        }

        // if the path element was not committed to the repository, just remove it from the DSet
        // and we're done
        if (!element.inRepo) {
            getRoomObj().removeFromPathElements(elementId);
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
                getRoomObj().removeFromPathElements(elementId);
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

        final PathElement element = getRoomObj().pathElements.get(elementId);
        if (element == null) {
            throw new InvocationException(SwiftlyCodes.E_INTERNAL_ERROR);
        }

        // if the path element was not committed to the repository, just rename it in the DSet
        // and we're done
        if (!element.inRepo) {
            element.setName(newName);
            getRoomObj().updatePathElements(element);
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
                getRoomObj().updatePathElements(element);
                listener.requestProcessed();
            }

            protected Exception _error;
        });
    }

    // from interface ProjectRoomProvider
    public void addDocument (ClientObject caller, String fileName, PathElement parent,
                             String mimeType, final ConfirmListener listener)
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
        getRoomObj().addPathElement(element);

        // add the swiftly document to the dest
        getRoomObj().addSwiftlyDocument(doc);

        listener.requestProcessed();
    }

    // from interface ProjectRoomProvider
    public void updateTextDocument (ClientObject caller, int elementId, String text,
                                    ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);

        getRoomObj().postEvent(
            new DocumentUpdatedEvent(getRoomObj().getOid(), caller.getOid(), elementId, text));

        listener.requestProcessed();
    }

    // from interface ProjectRoomProvider
    public void deleteDocument (ClientObject caller, int elementId, ConfirmListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);

        getRoomObj().removeFromDocuments(elementId);

        listener.requestProcessed();
    }

    // from interface ProjectRoomProvider
    public void buildProject (ClientObject caller, InvocationService.ResultListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);
        MemberObject memobj = (MemberObject)caller;

        AbstractBuildTask buildTask = new BuildTask(this, memobj.memberName, listener);
        _svnExecutor.addTask(new CommitProjectTask(this, buildTask, listener));
    }

    // from interface ProjectRoomProvider
    public void buildAndExportProject (ClientObject caller,
                                       InvocationService.ResultListener listener)
        throws InvocationException
    {
        // check that the caller has the correct permissions to perform this action
        requireWritePermissions(caller);
        MemberObject memobj = (MemberObject)caller;

        AbstractBuildTask buildTask = new BuildAndExportTask(this, memobj.memberName, listener);
        _svnExecutor.addTask(new CommitProjectTask(this, buildTask, listener));
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
                    getRoomObj().addSwiftlyDocument(_doc);
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
        PathElement element = getRoomObj().findPathElement(
            uploadFile.getOriginalName(), getRoomObj().getRootElement());
        if (element != null) {
            // let's try to pull the resolved document from the room object. this may return null
            // in which case the InsertFileUploadTask will load it from the repository
            SwiftlyDocument doc = getRoomObj().getDocument(element);
            _svnExecutor.addTask(new InsertFileUploadTask(
                this, uploadFile, element, doc, listener));

        // otherwise this is a new file
        } else {
            _svnExecutor.addTask(new InsertFileUploadTask(this, uploadFile, listener));
        }
    }

    /**
     * Used by the SwiftlyManager to update the local list of collaborators.
     */
    public void addCollaborator (MemberName name)
    {
        getRoomObj().addToCollaborators(name);
        getRoomObj().postMessage(ProjectRoomObject.ACCESS_CONTROL_CHANGE);

    }

    /**
     * Used by the SwiftlyManager to update the local list of collaborators.
     */
    public void removeCollaborator (MemberName name)
    {
        getRoomObj().removeFromCollaborators(name.getKey());
        getRoomObj().postMessage(ProjectRoomObject.ACCESS_CONTROL_CHANGE);
        getResultItems().remove(name);
    }

    /**
     * Used by the SwiftlyManager to update the room object project object.
     */
    public void updateProject (final SwiftlyProject project)
    {
        // if the name of the project has changed, update the root path element in the dset
        if (!getRoomObj().project.projectName.equals(project.projectName)) {
            getRoomObj().getRootElement().setName(project.projectName);
            getRoomObj().publishPathElement(getRoomObj().getRootElement());
        }

        getRoomObj().setProject(project);
        getRoomObj().postMessage(ProjectRoomObject.ACCESS_CONTROL_CHANGE);
    }

    /**
     * Throws an InvocationException if the caller should not be able to modify the project.
     */
    public void requireWritePermissions (ClientObject caller)
        throws InvocationException
    {
        MemberObject memobj = (MemberObject)caller;
        if (!getRoomObj().hasWriteAccess(memobj.memberName)) {
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
        if (!getRoomObj().hasReadAccess(memobj.memberName)) {
            throw new InvocationException(SwiftlyCodes.E_ACCESS_DENIED);
        }
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            SwiftlyDocument element = (SwiftlyDocument)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(getRoomObj().pathElements);
        } else if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            PathElement element = (PathElement)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(getRoomObj().pathElements);
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            SwiftlyDocument element = (SwiftlyDocument)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(getRoomObj().pathElements);
        } else if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            PathElement element = (PathElement)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(getRoomObj().pathElements);
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

    /**
     * Return the ProjectRoomObject resolved in this room manager.
     * Note: This should only be used on the distributed object thread.
     */
    public ProjectRoomObject getRoomObj ()
    {
        // Enforce access only on the distributed object thread.
        MsoyServer.requireDObjThread();
        return _roomObj;
    }

    /**
     * Return the cache of memberId to build result itemId mapping used for exporting results.
     * Note: This should only be used on the distributed object thread.
     */
    public Map<MemberName, Integer> getResultItems ()
    {
        // Enforce access only on the distributed object thread.
        MsoyServer.requireDObjThread();
        return _resultItems;
    }

    /**
     * Return the ProjectStorage being used by this room manager.
     */
    public ProjectStorage getStorage ()
    {
        return _storage;
    }

    /**
     * Return the ProjectBuilder being used by this room manager.
     */
    public ProjectBuilder getBuilder ()
    {
        return _builder;
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
        InvocationService.ResultListener listener = new InvocationService.ResultListener() {
            public void requestProcessed (Object result)
            {
                // nada. no result will be provided to the user.
            }
            public void requestFailed (String reason) {
                // nada. no result will be provided to the user.
            }

        };
        _svnExecutor.addTask(new CommitProjectTask(this, listener));
    }

    /** Server-root relative path to the Whirled SDK. */
    protected static final String WHIRLED_SDK = "/data/swiftly/whirled_sdk";

    /** Server-root relative path to the Flex SDK. */
    protected static final String FLEX_SDK = "/data/swiftly/flex_sdk";

    /** This is used to execute potentially long running svn operations serially on a separate
     * thread so that they do not interfere with normal server operation. */
    protected SerialExecutor _svnExecutor;

    /** Cache the memberId to build result itemId mapping used for exporting results */
    protected Map<MemberName, Integer> _resultItems;

    protected ProjectRoomObject _roomObj;
    protected ProjectStorage _storage;
    protected ProjectBuilder _builder;
    protected File _buildDir;

}
