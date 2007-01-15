//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.net.URL;

import com.threerings.presents.dobj.DSet;

/**
 * Represents an element of a project, either the root, a directory or a file element.
 */
public class PathElement
    implements DSet.Entry
{
    /** Indicates the type of this project element. */
    public enum Type { ROOT, DIRECTORY, FILE };

    /** Uniquely identifies this path element in the distributed state. */
    public int elementId;

    /**
     * Creates a project root element.
     */
    public static PathElement createRoot (String name)
    {
        return new PathElement(Type.ROOT, name, -1);
    }

    /**
     * Creates a directory element.
     */
    public static PathElement createDirectory (String name, int parentId)
    {
        return new PathElement(Type.DIRECTORY, name, parentId);
    }

    public PathElement ()
    {
    }

    public PathElement (Type type, String name, int parentId)
    {
        _type = type;
        setName(name);
        setParentId(parentId);
    }

    public Type getType ()
    {
        return _type;
    }

    public String getName ()
    {
        return _name;
    }

    public int getParentId ()
    {
        return _parentId;
    }

    /**
     * Constructs the URL by which this file element's contents can be loaded.
     */
    public URL getElementURL ()
    {
        return null; // TODO
    }

    public void setName (String name)
    {
        _name = name;
    }

    public void setParentId (int parentId)
    {
        _parentId = parentId;
    }

    // from interface Dset.Key
    public Comparable getKey ()
    {
        return elementId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof PathElement) {
            return elementId == ((PathElement)other).elementId;
        } else {
            return false;
        }
    }

    @Override // from Object
    public int hashCode ()
    {
        return elementId;
    }

    @Override // from Object
    public String toString ()
    {
        return getName();
    }

    protected Type _type;
    protected String _name;
    protected int _parentId = -1;
}
