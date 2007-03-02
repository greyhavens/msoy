//
// $Id$

package com.threerings.msoy.swiftly.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

/**
 * Maintains the distributed state for a Swiftly project.
 */
public class ProjectRoomObject extends PlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>name</code> field. */
    public static final String NAME = "name";

    /** The field name of the <code>pathElements</code> field. */
    public static final String PATH_ELEMENTS = "pathElements";

    /** The field name of the <code>documents</code> field. */
    public static final String DOCUMENTS = "documents";

    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>console</code> field. */
    public static final String CONSOLE = "console";
    // AUTO-GENERATED: FIELDS END

    /** The name of the project being edited. */
    public String name;

    /** All resolved elements in this project. */
    public DSet<PathElement> pathElements = new DSet<PathElement>();

    /** All loaded Swiftly Documents in this project. */
    public DSet<SwiftlyDocument> documents = new DSet<SwiftlyDocument>();

    /** Provides invocation services. */
    public ProjectRoomMarshaller service;

    /** Used to broadcast console (compiler output, etc.) messages to all members of the room. */
    public String console;

    /**
     * Adds a new path element to the project's distributed state, assigning a unique identifier to
     * it and then adding it to the distributed state.
     */
    public void addPathElement (PathElement element)
    {
        element.elementId = _nextElementId++;
        addToPathElements(element);
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

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>name</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setName (String value)
    {
        String ovalue = this.name;
        requestAttributeChange(
            NAME, value, ovalue);
        this.name = value;
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

    /**
     * Requests that the <code>console</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setConsole (String value)
    {
        String ovalue = this.console;
        requestAttributeChange(
            CONSOLE, value, ovalue);
        this.console = value;
    }
    // AUTO-GENERATED: METHODS END

    /** Used to assign unique identifiers to elements. */
    protected transient int _nextElementId;
}
