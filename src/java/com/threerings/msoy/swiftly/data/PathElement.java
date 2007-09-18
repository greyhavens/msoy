//
// $Id$

package com.threerings.msoy.swiftly.data;

import static com.threerings.msoy.Log.log;

import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;

import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DSet;

/**
 * Represents an element of a project, either the root, a directory or a file element.
 */
public class PathElement
    implements DSet.Entry, Comparable<PathElement>
{
    /** Indicates the type of this project element. */
    public enum Type { ROOT, DIRECTORY, FILE };

    /** Uniquely identifies this path element in the distributed state. */
    public int elementId;

    /** Indicates whether this path element has been committed to the repository or not. */
    public transient boolean inRepo;

    /**
     * Creates a project root element.
     */
    public static PathElement createRoot (String name)
    {
        return new PathElement(Type.ROOT, name, null, null);
    }

    /**
     * Creates a directory element.
     */
    public static PathElement createDirectory (String name, PathElement parent)
    {
        return new PathElement(Type.DIRECTORY, name, parent, null);
    }

    /**
     * Creates a file element.
     */
    public static PathElement createFile (String name, PathElement parent, String mimeType)
    {
        return new PathElement(Type.FILE, name, parent, mimeType);
    }

    public PathElement ()
    {
    }

    public PathElement (Type type, String name, PathElement parent, String mimeType)
    {
        _type = type;
        setName(name);
        setParent(parent);
        setMimeType(mimeType);
    }

    /**
     * After serialization, call lazarus with the pathElements DSet
     * to correctly re-bind the any transient instance variables. This
     * relies on parent nodes being added to the DSet prior to their children,
     * and an assert() below makes sure of that.
     */
    public void lazarus (DSet<PathElement> pathElements) {
        if (_parentKey != null) {
            _parent = pathElements.get(_parentKey);
            assert(_parent != null);
        }
    }

    public Type getType ()
    {
        return _type;
    }

    public String getName ()
    {
        return _name;
    }

    public String getMimeType ()
    {
        return _mimeType;
    }

    public PathElement getParent ()
    {
        return _parent;
    }

    /**
     * Returns the absolute path to this PathElement, based on the project root.
     */
    public String getAbsolutePath ()
    {
        PathElement node;
        StringBuffer output = new StringBuffer();
        List<PathElement> pathList = new ArrayList<PathElement>();

        // This is a relatively expensive implementation, but then, it always is

        // We build up a list of parent elements, reverse the list, append them all
        // together and return the result. We skip the root node, since it doesn't actually
        // represent the path.
        for (node = this; node != null; node = node.getParent()) {
            if (node.getType() == Type.ROOT) {
                continue;
            }

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

    /**
     * Returns the ImageIcon associated with this type of path element.
     */
    public ImageIcon getIcon ()
    {
        if (_icon == null) {
            try {
                _icon = SwiftlyDocument.createIcon(this);

            } catch (IOException ioe) {
                log.warning("Failed to set document icon. " + ioe);
            }
        }
        return _icon;
    }

    public void setName (String name)
    {
        _name = name;
    }

    public void setMimeType (String mimeType)
    {
        _mimeType = mimeType;
    }

    public void setParent (PathElement parent)
    {
        _parent = parent;
    }

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return elementId;
    }

    // from Comparable
    // note: lower number returned means this element will be higher up the project tree
    public int compareTo (PathElement other)
    {
        // if the elements are equal, then they compare as equals as well
        if (other.equals(this)) {
            return 0;
        }

        // if either element is the root, it wins
        if (this.getType() == Type.ROOT) {
            return -1;
        } else if (other.getType() == Type.ROOT) {
            return 1;
        }

        // if the elements have the same parent,
        if (this.getParent().equals(other.getParent())) {
            // and they are both directories or files, compare their names
            if (this.getType() == other.getType()) {
                // TODO: if they are both files, compare mimetypes or if mimetype is same names?
                return this.getName().compareTo(other.getName());

            // otherwise, the directory wins
            } else {
                return this.getType() == Type.DIRECTORY ? -1 : 1;
            }
        }

        // if all else fails, compare the parents, which will recurse
        return this.getParent().compareTo(other.getParent());
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof PathElement) {
            // This isn't necessarily the best way to determine equality, but it will be correct
            // within a given tree of path elements.
            return getAbsolutePath().equals(((PathElement)other).getAbsolutePath());
        } else {
            return false;
        }
    }

    @Override // from Object
    public int hashCode ()
    {
        return getAbsolutePath().hashCode();
    }

    @Override // from Object
    public String toString ()
    {
        return getName();
    }

    /**
     * Store the parent's key prior to serialization, such that we can use it to re-bind
     * the transient parent instance variable when lazarus() is called on the other side
     * of the wire, post-serialization.
     */
    public void writeObject(ObjectOutputStream out)
        throws IOException
    {
        if (_parent != null) {
            _parentKey = _parent.getKey();
        }
        out.defaultWriteObject();
    }

    /** Directory, File, or Root. */
    protected Type _type;

    /** Relative file name. */
    protected String _name;

    /** Mime type, may be null if unknown. */
    protected String _mimeType;

    /** Enclosing parent PathElement, if any. */
    protected transient PathElement _parent = null;

    /** The icon associated with this type of path element. */
    protected transient ImageIcon _icon;

    /** Key for the enclosing parent, used to re-bind the transient _parent instance variable
     * post-serialization. */
    private Comparable _parentKey = null;
}
