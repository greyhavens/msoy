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

    /** The field name of the <code>elements</code> field. */
    public static final String ELEMENTS = "elements";

    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";
    // AUTO-GENERATED: FIELDS END

    /** The name of the project being edited. */
    public String name;

    /** All resolved elements in this project. */
    public DSet<PathElement> elements = new DSet<PathElement>();

    /** Provides invocation services. */
    public ProjectRoomMarshaller service;

    /**
     * Adds a new path element to the project's distributed state, assigning a unique identifier to
     * it and then adding it to the distributed state.
     */
    public void addPathElement (PathElement element)
    {
        element.elementId = _nextElementId++;
        addToElements(element);
    }

    /**
     * Returns the root path element of this project.
     */
    public PathElement getRootElement ()
    {
        for (PathElement element : elements) {
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
     * <code>elements</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToElements (PathElement elem)
    {
        requestEntryAdd(ELEMENTS, elements, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>elements</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromElements (Comparable key)
    {
        requestEntryRemove(ELEMENTS, elements, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>elements</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateElements (PathElement elem)
    {
        requestEntryUpdate(ELEMENTS, elements, elem);
    }

    /**
     * Requests that the <code>elements</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setElements (DSet<com.threerings.msoy.swiftly.data.PathElement> value)
    {
        requestAttributeChange(ELEMENTS, value, this.elements);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.swiftly.data.PathElement> clone =
            (value == null) ? null : value.typedClone();
        this.elements = clone;
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
}
