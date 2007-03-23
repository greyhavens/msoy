//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.swiftly.client.SwiftlyDocumentEditor;

/**
 * Represents a source file in a project and contains the text of the file.
 * Text is stored in the provided encoding, and each document retains an
 * unmodified copy of its initial data to allow for delta generation.
 */
public abstract class SwiftlyDocument
    implements DSet.Entry
{    
    /** Uniquely identifies this document element in the distributed state. */
    public int elementId;

    public SwiftlyDocument ()
    {
    }

    /** Commit the in memory data to the file backing. */
    public abstract void commit () throws IOException;

    /** Check to see if the document has changed. */
    public abstract boolean isDirty () throws IOException;

    /** Tell the supplied editor to load this document. */
    public abstract void loadInEditor (SwiftlyDocumentEditor editor);

    public Comparable getKey ()
    {
        return elementId;
    }

    public PathElement getPathElement ()
    {
        return _path;
    }

    /** Returns an stream corresponding to the unmodified data. */
    public abstract InputStream getOriginalData () throws IOException;

    /** Returns an InputStream corresponding to the modified data. */
    public abstract InputStream getModifiedData () throws IOException;

    /**
     * After serialization, call lazarus with the DSet of associated pathElements
     * to correctly re-bind the any transient instance variables. This
     * relies on PathElement nodes being added to the DSet prior to their associated
     * SwiftlyDocuments, and an assert() below makes sure of that.
     */
    public void lazarus (DSet<PathElement> pathElements) {
        if (_pathKey != null) {
            _path = pathElements.get(_pathKey);
            assert(_path != null);
        }
    }

    /**
     * Store the PathElement's key prior to serialization, such that we can use it to re-bind
     * the transient PathElement instance variable when lazarus() is called on the other side
     * of the wire, post-serialization.
     */
    public void writeObject(ObjectOutputStream out)
        throws IOException
    {
        if (_path != null) {
            _pathKey = _path.getKey();            
        }
        out.defaultWriteObject();
    }

    @Override // from Object
    public String toString ()
    {
        return _path.getName();
    }

    /** Reference to our associated path element. */
    protected transient PathElement _path = null;

    /** Key for the associated PathElement, used to re-bind the transient _path instance variable
     * post-serialization. */
    private Comparable _pathKey = null;
}
