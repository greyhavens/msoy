//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.ImageIcon;

import org.apache.commons.io.IOUtils;

import com.threerings.io.ObjectOutputStream;
import com.threerings.msoy.swiftly.client.controller.SwiftlyDocumentEditor;
import com.threerings.msoy.swiftly.client.view.PositionLocation;
import com.threerings.presents.dobj.DSet;

/**
 * Represents a source file in a project and contains the text of the file.
 * Text is stored in the provided encoding, and each document retains an
 * unmodified copy of its initial data to allow for delta generation.
 */
public abstract class SwiftlyDocument
    implements DSet.Entry, Cloneable
{
    /** Uniquely identifies this document element in the distributed state. */
    public int documentId;

    /** Implemented by concrete subclasses to support construction based on mime type */
    public interface DocumentFactory {
        /** Returns true if the provided mime type is supported. */
        public boolean handlesMimeType (String mimeType);

        /** Returns an ImageIcon for displaying this kind of document */
        public ImageIcon createIcon () throws IOException;

        /** Construct a new, blank SwiftlyDocument. */
        public SwiftlyDocument createDocument (PathElement path,
            String encoding) throws IOException;

        /** Construct a new SwiftlyDocument. */
        public SwiftlyDocument createDocument (InputStream data, PathElement path,
            String encoding) throws IOException;
    }

    /**
     * Returns a new, blank SwiftlyDocument using the supplied PathElement's mimeType;
     */
    public static SwiftlyDocument createFromPathElement (PathElement path, String encoding)
        throws IOException
    {
        return createFromPathElement(null, path, encoding);
    }

    /**
     * Returns a new SwiftlyDocument using the supplied PathElement's mimeType.
     */
    public static SwiftlyDocument createFromPathElement (InputStream data, PathElement path,
        String encoding)
        throws IOException
    {
        for (DocumentFactory factory : _documentTypeFactories) {
            if (factory.handlesMimeType(path.getMimeType())) {
                if (data != null) {
                    return factory.createDocument(data, path, encoding);
                } else {
                    return factory.createDocument(path, encoding);
                }
            }
        }

        // BinaryDocument handles all mime types so this statement should never be reached
        throw new RuntimeException("Unhandled mime-type. SwiftlyBinaryDocument should handle" +
            "all mime types");
    }

    /**
     * Returns an ImageIcon for displaying a SwiftlyDocument using the supplied PathElement's
     * mimeType.
     *
     */
    public static ImageIcon createIcon (PathElement path)
        throws IOException
    {
        for (DocumentFactory factory : _documentTypeFactories) {
            if (factory.handlesMimeType(path.getMimeType())) {
                return factory.createIcon();
            }
        }

        // BinaryDocument handles all mime types so this statement should never be reached
        throw new RuntimeException("Unhandled mime-type. SwiftlyBinaryDocument should handle" +
            "all mime types");
    }

    /** Required for the dobj system. Do not use. */
    public SwiftlyDocument ()
    {
    }

    /** Initializes the SwiftlyDocument. */
    public SwiftlyDocument (InputStream data, PathElement path)
        throws IOException
    {
        _path = path;

        // Create our backing store file
        _backingStore = File.createTempFile("swiftlydocument", ".basefile");
        _backingStore.deleteOnExit();
    }

    /** Commit the in memory data to the file backing. */
    public void commit ()
        throws IOException
    {
        // copy our modified data over our pristine backing store data
        FileOutputStream fileOutput = new FileOutputStream(_backingStore);
        try {
            IOUtils.copy(getModifiedData(), fileOutput);
        } finally {
            fileOutput.close();
        }
    }

    /** Replaces the data for this document. */
    public abstract void setData (InputStream data, String encoding) throws IOException;

    /** Check to see if the document has changed. */
    public abstract boolean isDirty () throws IOException;

    /**
     * Tell the supplied editor to load this document, at the supplied row and column.
     * @param highlight indicates whether the new location should be highlighted briefly
     */
    public abstract void loadInEditor (SwiftlyDocumentEditor editor, PositionLocation location);

    public Comparable getKey ()
    {
        return documentId;
    }

    public PathElement getPathElement ()
    {
        return _path;
    }

    /** Returns an stream corresponding to the unmodified data. */
    public InputStream getOriginalData ()
        throws IOException
    {
        return new FileInputStream(_backingStore);
    }

    /** Returns an InputStream corresponding to the modified data. */
    public abstract InputStream getModifiedData () throws IOException;

    /**
     * After serialization, call lazarus with the DSet of associated pathElements to correctly
     * re-bind the any transient instance variables. This relies on PathElement nodes being added
     * to the DSet prior to their associated SwiftlyDocuments, and an assert() below makes sure of
     * that.
     */
    public void lazarus (DSet<PathElement> pathElements)
    {
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
    public void writeObject (ObjectOutputStream out)
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
        return documentId + ":" + ((_path == null) ? "<unknown>" : _path.getName());
    }

    @Override // from Object
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    @Override // from Object
    protected void finalize ()
        throws Throwable
    {
        // be sure to delete our backing store
        try {
            if (_backingStore != null) {
                _backingStore.delete();
            }
        } finally {
            super.finalize();
        }
    }

    /** Reference to our associated path element. */
    protected transient PathElement _path;

    /** Unmodified disk-backing of the document data. */
    protected transient File _backingStore;

    /** Key for the associated PathElement, used to re-bind the transient _path instance variable
     *  post-serialization. */
    protected Comparable _pathKey;

    /** Instances of all the SwiftlyDocument factories. Order determines mime-type handling precedence. */
    protected static DocumentFactory[] _documentTypeFactories = {
        new SwiftlyTextDocument.DocumentFactory(),
        new SwiftlyImageDocument.DocumentFactory(),
        new SwiftlyBinaryDocument.DocumentFactory()
    };
}
