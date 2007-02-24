//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        return new PathElement(Type.ROOT, name, null);
    }

    /**
     * Creates a directory element.
     */
    public static PathElement createDirectory (String name, PathElement parent)
    {
        return new PathElement(Type.DIRECTORY, name, parent);
    }

    /**
     * Creates a file element.
     */
    public static PathElement createFile (String name, PathElement parent)
    {
        return new PathElement(Type.FILE, name, parent);
    }

    public PathElement ()
    {
    }

    public PathElement (Type type, String name, PathElement parent)
    {
        _type = type;
        setName(name);
        setParent(parent);
    }

    public Type getType ()
    {
        return _type;
    }

    public String getName ()
    {
        return _name;
    }

    public PathElement getParent ()
    {
        return _parent;
    }

    public String getAbsolutePath ()
    {
        PathElement node;
        StringBuffer output = new StringBuffer();
        List<PathElement> pathList = new ArrayList<PathElement>();
        
        // This is a relatively expensive implementation, but then, it always is

        // We build up a list of parent elements, reverse the list, append them all
        // together and return the result.
        for (node = this; node != null; node = node.getParent()) {
            pathList.add(node);
        }
        Collections.reverse(pathList);

        for (PathElement element : pathList) {
            output.append("/" + element.getName());   
        }

        return output.toString();
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

    public void setParent (PathElement parent)
    {
        _parent = parent;
    }

    // from interface Dset.Entry
    public Comparable getKey ()
    {
        return elementId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof PathElement) {
            // This isn't necessarily the best way to determine equality, but it will be correct within
            // a given tree of path elements.
            return getAbsolutePath().equals(((PathElement)other).getAbsolutePath());
        } else {
            return false;
        }
    }

    @Override // from Object
    public String toString ()
    {
        return getName();
    }

    protected Type _type;
    protected String _name;
    protected PathElement _parent = null;
}
