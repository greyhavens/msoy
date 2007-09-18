//
// $Id$

package com.threerings.msoy.swiftly.client.model;

import static com.threerings.msoy.Log.log;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.swiftly.client.SwiftlyContext;
import com.threerings.msoy.swiftly.client.controller.DocumentContentListener;
import com.threerings.msoy.swiftly.client.controller.NewPathElement;
import com.threerings.msoy.swiftly.client.event.PathElementListener;
import com.threerings.msoy.swiftly.client.event.SwiftlyDocumentListener;
import com.threerings.msoy.swiftly.client.model.DocumentModelDelegate.FailureCode;
import com.threerings.msoy.swiftly.client.view.PositionLocation;
import com.threerings.msoy.swiftly.data.DocumentUpdateListener;
import com.threerings.msoy.swiftly.data.DocumentUpdatedEvent;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyImageDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

/**
 * Manages SwiftlyDocuments and PathElements in a given SwiftlyProject.
 */
public class DocumentModel
    implements SetListener, DocumentUpdateListener
{
    public DocumentModel (ProjectRoomObject roomObj, SwiftlyContext ctx)
    {
        _roomObj = roomObj;
        _client = ctx.getClient();
        _member = ((MemberObject)ctx.getClient().getClientObject()).memberName;
        _roomObj.addListener(this);

        // raise all bindings initially during construction
        lazarusPathElements();
        lazarusDocuments();
    }

    /**
     * Return the PathElement representing the root of the project.
     */
    public PathElement getRootElement ()
    {
        return _roomObj.getRootElement();
    }

    /**
     * Return the set of PathElements.
     */
    public Set<PathElement> getPathElements ()
    {
        Set<PathElement> set = new TreeSet<PathElement>();
        for (PathElement element : _roomObj.pathElements) {
            set.add(element);
        }
        return set;
    }

    /**
     * Returns the PathElement associated with the supplied path string, or null if no
     * PathElement matches.
     */
    public PathElement findPathElementByPath (String path)
    {
        return _roomObj.findPathElementByPath(path);
    }

    /**
     * Returns true if the given PathElement can be renamed by the current user.
     */
    public boolean isRenameable (PathElement element)
    {
        if (!_roomObj.hasWriteAccess(_member)) {
            return false;
        }

        if (_roomObj.project.getTemplateSourceName().equals(element.getName()) ||
            element.getType() == PathElement.Type.ROOT) {
            return false;
        }

        return true;
    }

    /**
     * Returns true if the given PathElement can be deleted by the current user.
     */
    public boolean isDeleteable (PathElement element)
    {
        if (!_roomObj.hasWriteAccess(_member)) {
            return false;
        }

        if (_roomObj.project.getTemplateSourceName().equals(element.getName()) ||
            element.getType() == PathElement.Type.ROOT) {
            return false;
        }

        return true;
    }

    /**
     * Returns true if the fileName with the given parent already exists in the project.
     */
    public boolean pathElementExists (String fileName, PathElement parentElement)
    {
        return _roomObj.pathElementExists(fileName, parentElement);
    }

    /**
     * Requests that the supplied fileName,parent,mimeType be used to create a new PathElement and
     * add that PathElement to the project.
     */
    public void addDocument (final RequestId requestId, final NewPathElement newElement,
                             final DocumentModelDelegate delegate)
    {
        _roomObj.service.addDocument(_client, newElement.name, newElement.parent,
            newElement.mimeType, new ConfirmListener () {
            public void requestProcessed ()
            {
                delegate.documentAdded(requestId, newElement);
            }

            public void requestFailed (String reason)
            {
                delegate.documentAdditionFailed(
                    requestId, newElement, FailureCode.ADD_DOCUMENT_FAILED);
            }
        });
    }

    /**
     * Requests that the supplied PathElement be renamed using the supplied String.
     */
    public void renamePathElement (final RequestId requestId, final PathElement element,
                                   final String newName, final DocumentModelDelegate delegate)
    {
        _roomObj.service.renamePathElement(_client, element.elementId, newName,
            new ConfirmListener () {
            public void requestProcessed () {
                delegate.pathElementRenamed(requestId, element);
            }
            public void requestFailed (String reason) {
                delegate.pathElementRenameFailed(
                    requestId, element, FailureCode.RENAME_ELEMENT_FAILED);
            }
        });
    }

    /**
     * Requests that the supplied PathElement which references a SwiftlyDocument be deleted.
     */
    public void deleteDocument (final RequestId requestId, final PathElement element,
                                final DocumentModelDelegate delegate)
    {
        _roomObj.service.deletePathElement(_client, element.elementId, new ConfirmListener () {
            public void requestProcessed () {
                delegate.documentDeleted(requestId, element);
            }
            public void requestFailed (String reason) {
                delegate.documentDeleteFailed(
                    requestId, element, FailureCode.RENAME_ELEMENT_FAILED);
            }
        });
    }

    /**
     * Sends a message to the server reporting that the given SwiftlyTextDocument should have its
     * text replaced with the supplied string.
     */
    public void updateTextDocument (final RequestId requestId, final SwiftlyTextDocument doc,
                                    final String text, final DocumentModelDelegate delegate)
    {
        _roomObj.service.updateTextDocument(_client, doc.documentId, text, new ConfirmListener () {
            public void requestProcessed ()
            {
                delegate.textDocumentUpdated(requestId, doc);
            }

            public void requestFailed (String reason)
            {
                delegate.textDocumentUpdateFailed(requestId, doc,
                    FailureCode.UPDATE_DOCUMENT_FAILED);
            }
        });
    }

    /**
     * Request that the SwiftlyDocument associated with the given PathElement be loaded.
     */
    public void loadDocument (final RequestId requestId, final PathElement element,
                              final PositionLocation location,
                              final DocumentModelDelegate delegate)
    {
        // If the document is already in the dset, return the loaded document.
        SwiftlyDocument doc = _roomObj.getDocument(element);
        if (doc != null) {
            delegate.documentLoaded(requestId, doc, location);
            return;
        }

        // Otherwise load the document from the backend.
        _roomObj.service.loadDocument(_client, element, new ConfirmListener() {
            public void requestProcessed () {
                SwiftlyDocument doc = _roomObj.getDocument(element);
                if (doc == null) {
                    delegate.documentLoadingFailed(
                        requestId, element, FailureCode.LOAD_DOCUMENT_FAILED);
                } else {
                    delegate.documentLoaded(requestId, doc, location);
                }
            }
            public void requestFailed (String reason) {
                delegate.documentLoadingFailed(
                    requestId, element, FailureCode.LOAD_DOCUMENT_FAILED);
            }
        });
    }

    /** Called to add a PathElementListener. */
    public void addPathElementListener (PathElementListener listener)
    {
        _pathElementListeners.add(listener);
    }

    /** Called to remove a PathElementListener. */
    public void removePathElementListener (PathElementListener listener)
    {
        _pathElementListeners.remove(listener);
    }

    /** Called to add a SwiftlyDocumentListener. */
    public void addSwiftlyDocumentListener (SwiftlyDocumentListener listener)
    {
        _swiftlyDocumentListeners.add(listener);
    }

    /** Called to remove a SwiftlyDocumentListener. */
    public void removeSwiftlyDocumentListener (SwiftlyDocumentListener listener)
    {
        _swiftlyDocumentListeners.remove(listener);
    }

    /** Called to add a DocumentContentsListener. */
    public void addDocumentContentsListener (DocumentContentListener listener)
    {
        _documentContentListeners.add(listener);
    }

    /** Called to remove a DocumentContentsListener. */
    public void removeDocumentContentsListener (DocumentContentListener listener)
    {
        _documentContentListeners.remove(listener);
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            SwiftlyDocument doc = (SwiftlyDocument)event.getEntry();
            // Re-bind transient instance variables
            doc.lazarus(_roomObj.pathElements);

            for (SwiftlyDocumentListener listener : _swiftlyDocumentListeners) {
                listener.documentAdded(doc);
            }

        } else if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            PathElement element = (PathElement)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);

            for (PathElementListener listener : _pathElementListeners) {
                listener.elementAdded(element);
            }
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            SwiftlyDocument doc = (SwiftlyDocument)event.getEntry();
            // Re-bind transient instance variables
            doc.lazarus(_roomObj.pathElements);

            if (doc instanceof SwiftlyTextDocument) {
                for (SwiftlyDocumentListener listener : _swiftlyDocumentListeners) {
                    listener.documentUpdated((SwiftlyTextDocument)doc);
                }
            } else if (doc instanceof SwiftlyImageDocument) {
                for (SwiftlyDocumentListener listener : _swiftlyDocumentListeners) {
                    listener.documentUpdated((SwiftlyImageDocument)doc);
                }
            }

        } else if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            PathElement element = (PathElement)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);

            for (PathElementListener listener : _pathElementListeners) {
                listener.elementUpdated(element);
            }
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            SwiftlyDocument doc = (SwiftlyDocument)event.getOldEntry();
            for (SwiftlyDocumentListener listener : _swiftlyDocumentListeners) {
                listener.documentRemoved(doc);
            }

        } else if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            PathElement element = (PathElement)event.getOldEntry();
            for (PathElementListener listener : _pathElementListeners) {
                listener.elementRemoved(element);
            }
        }
    }

    // from DocumentUpdateListener
    public void documentUpdated (DocumentUpdatedEvent event)
    {
        // if this client was the sender of the event, ignore it.
        if (_client.getClientOid() == event.getEditorOid()) {
            return;
        }

        SwiftlyDocument doc = _roomObj.documents.get(event.getDocumentId());

        // These events are only for text documents.
        if (!(doc instanceof SwiftlyTextDocument)) {
            log.warning("Received DocumentUpdatedEvent for non SwiftlyTextDocument.");
            return;
        }

        for (DocumentContentListener listener : _documentContentListeners) {
            listener.documentContentsChanged((SwiftlyTextDocument)doc);
        }
    }

    private void lazarusPathElements ()
    {
        // Raise all path elements from the dead, re-binding transient
        // instance variables.
        for (PathElement element : _roomObj.pathElements) {
            element.lazarus(_roomObj.pathElements);
        }
    }

    private void lazarusDocuments ()
    {
        // Raise all swiftly documents from the dead, re-binding transient
        // instance variables.
        for (SwiftlyDocument doc : _roomObj.documents) {
            doc.lazarus(_roomObj.pathElements);
        }
    }

    /** A set of components listening for PathElement changes. */
    private final Set<PathElementListener> _pathElementListeners =
        new HashSet<PathElementListener>();

    /** A set of components listening for SwiftlyDocument changes. */
    private final Set<SwiftlyDocumentListener> _swiftlyDocumentListeners =
        new HashSet<SwiftlyDocumentListener>();

    /** A set of components listening for SwiftlyTextDocument content changes. */
    private final Set<DocumentContentListener> _documentContentListeners =
        new HashSet<DocumentContentListener>();

    private final ProjectRoomObject _roomObj;
    private final Client _client;
    private final MemberName _member;
}
