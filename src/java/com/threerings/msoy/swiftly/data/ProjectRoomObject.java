//
// $Id$

package com.threerings.msoy.swiftly.data;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.presents.dobj.DSet;

/**
 * Maintains the distributed state for a Swiftly project.
 */
public class ProjectRoomObject extends PlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>project</code> field. */
    public static final String PROJECT = "project";

    /** The field name of the <code>pathElements</code> field. */
    public static final String PATH_ELEMENTS = "pathElements";

    /** The field name of the <code>documents</code> field. */
    public static final String DOCUMENTS = "documents";

    /** The field name of the <code>collaborators</code> field. */
    public static final String COLLABORATORS = "collaborators";

    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";
    // AUTO-GENERATED: FIELDS END

    /** Used to send access control change event messages. */
    public static final String ACCESS_CONTROL_CHANGE = "access_control_change";

    /** The SwiftlyProject being edited. */
    public SwiftlyProject project;

    /** All resolved elements in this project. */
    public DSet<PathElement> pathElements = new DSet<PathElement>();

    /** All loaded Swiftly Documents in this project. */
    public DSet<SwiftlyDocument> documents = new DSet<SwiftlyDocument>();

    /** All the collaborators for this project. */
    public DSet<MemberName> collaborators = new DSet<MemberName>();

    /** Provides invocation services. */
    public ProjectRoomMarshaller service;

    /**
     * Adds a new path element to the project's distributed state, assigning a unique identifier to
     * it and then adding it to the distributed state.
     */
    public void addPathElement (PathElement element)
    {
        element.elementId = ++_nextElementId;
        addToPathElements(element);
    }

    /**
     * Adds a new swiftly document to the project's distributed state, assigning a unique
     * identifier to it and then adding it to the distributed state.
     */
    public void addSwiftlyDocument (SwiftlyDocument doc)
    {
        doc.documentId = ++_nextDocumentId;
        addToDocuments(doc);
    }

    /**
     * Given a name and a parent, search the existing path elements for that element.
     * Return the element if found, null otherwise
     */
    public PathElement findPathElement (String name, PathElement parent)
    {
        for (PathElement elem : pathElements) {
            PathElement foundParent = elem.getParent();

            // the root was found, but caller was not asking for the root so continue
            if (foundParent == null && parent != null) {
                continue;
            }

            // if foundParent is null, elem is the root. If the requested parent is also null,
            // the caller was looking for the root, so compare the names. otherwise, check to see
            // if the found elements parent matches the supplied parent and then check the names
            if ((foundParent == null && parent == null) ||
                 foundParent.equals(parent)) {
                // if that element's name matches the name provided, we have found the element
                if (elem.getName().equals(name)) {
                    return elem;
                }
            }
        }

        // return null if we did not find the element
        return null;
    }

    /**
     * Given a full path string, search the existing path elements for that element.
     * Return the element if found, null otherwise
     */
    public PathElement findPathElementByPath (String path)
    {
        for (PathElement elem : pathElements) {
            if (elem.getAbsolutePath().equals(path)) {
                return elem;
            }
        }

        // return null if we did not find the element
        return null;
    }

    /**
     *  Publish the supplied SwiftlyDocument into the room object
     */
    public void publishSwiftlyDocument (final SwiftlyDocument doc)
    {
        if (doc.documentId == 0) {
            addSwiftlyDocument(doc);
        } else {
            updateDocuments(doc);
        }
    }

    /**
     *  Publish the supplied PathElement into the room object
     */
    public void publishPathElement (final PathElement element)
    {
        if (element.elementId == 0) {
            addPathElement(element);
        } else {
            updatePathElements(element);
        }
    }

    /**
     * Given a name and a parent, search the existing path elements for that element.
     * Return true if found, false otherwise
     */
    public boolean pathElementExists (String name, PathElement parent)
    {
        return findPathElement(name, parent) != null;
    }

    /**
     * Returns the resolved document for the supplied path element or null if the document is not
     * yet resolved or the path element does not correspond to a document (is a directory).
     */
    public SwiftlyDocument getDocument (PathElement element)
    {
        for (SwiftlyDocument doc : documents) {
            if (doc.getPathElement().elementId == element.elementId) {
                return doc;
            }
        }
        return null;
    }

    /**
     * Returns the root path element of this project.
     */
    public PathElement getRootElement ()
    {
        for (PathElement element : pathElements) {
            if (element.getType() == PathElement.Type.ROOT) {
                return element;
            }
        }
        return null;
    }

    /**
     * Returns true if the supplied MemberName has write access on the project.
     */
    public boolean hasWriteAccess (MemberName member)
    {
        if (collaborators.contains(member)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the supplied MemberName has read access on the project.
     */
    public boolean hasReadAccess (MemberName member)
    {
        if (project.remixable) {
            return true;
        }
        if (hasWriteAccess(member)) {
            return true;
        }
        return false;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>project</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setProject (SwiftlyProject value)
    {
        SwiftlyProject ovalue = this.project;
        requestAttributeChange(
            PROJECT, value, ovalue);
        this.project = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>pathElements</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPathElements (PathElement elem)
    {
        requestEntryAdd(PATH_ELEMENTS, pathElements, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>pathElements</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPathElements (Comparable key)
    {
        requestEntryRemove(PATH_ELEMENTS, pathElements, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>pathElements</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePathElements (PathElement elem)
    {
        requestEntryUpdate(PATH_ELEMENTS, pathElements, elem);
    }

    /**
     * Requests that the <code>pathElements</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPathElements (DSet<com.threerings.msoy.swiftly.data.PathElement> value)
    {
        requestAttributeChange(PATH_ELEMENTS, value, this.pathElements);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.swiftly.data.PathElement> clone =
            (value == null) ? null : value.typedClone();
        this.pathElements = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>documents</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToDocuments (SwiftlyDocument elem)
    {
        requestEntryAdd(DOCUMENTS, documents, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>documents</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromDocuments (Comparable key)
    {
        requestEntryRemove(DOCUMENTS, documents, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>documents</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateDocuments (SwiftlyDocument elem)
    {
        requestEntryUpdate(DOCUMENTS, documents, elem);
    }

    /**
     * Requests that the <code>documents</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setDocuments (DSet<com.threerings.msoy.swiftly.data.SwiftlyDocument> value)
    {
        requestAttributeChange(DOCUMENTS, value, this.documents);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.swiftly.data.SwiftlyDocument> clone =
            (value == null) ? null : value.typedClone();
        this.documents = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>collaborators</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToCollaborators (MemberName elem)
    {
        requestEntryAdd(COLLABORATORS, collaborators, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>collaborators</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromCollaborators (Comparable key)
    {
        requestEntryRemove(COLLABORATORS, collaborators, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>collaborators</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateCollaborators (MemberName elem)
    {
        requestEntryUpdate(COLLABORATORS, collaborators, elem);
    }

    /**
     * Requests that the <code>collaborators</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setCollaborators (DSet<com.threerings.msoy.data.all.MemberName> value)
    {
        requestAttributeChange(COLLABORATORS, value, this.collaborators);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.all.MemberName> clone =
            (value == null) ? null : value.typedClone();
        this.collaborators = clone;
    }

    /**
     * Requests that the <code>service</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setService (ProjectRoomMarshaller value)
    {
        ProjectRoomMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }
    // AUTO-GENERATED: METHODS END

    /** Used to assign unique identifiers to elements. */
    protected transient int _nextElementId;

    /** Used to assign unique identifiers to documents. */
    protected transient int _nextDocumentId;
}
