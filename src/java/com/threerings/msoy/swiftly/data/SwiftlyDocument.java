//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DSet;

/**
 * Represents a source file in a project and contains the text of the file.
 * Text is stored in the provided encoding, and each document retains an
 * unmodified copy of its initial data to allow for delta generation.
 */
public class SwiftlyDocument
    implements DSet.Entry
{    
    /** Uniquely identifies this document element in the distributed state. */
    public int elementId;

    public SwiftlyDocument ()
    {
    }

    /**
     * Instantiate a new SwiftlyDocument
     */
    public SwiftlyDocument (InputStream data, PathElement path, String encoding)
        throws IOException
    {
        StringBuffer textBuffer;
        FileOutputStream fileOutput;
        byte[] buf = new byte[1024];
        int len;

        // Store the pathelemtn
        _path = path;

        // Load and save the base document data.
        // TODO: Stack of deltas and a mmap()'d base document, such that we
        // don't waste RAM storing the whole file in memory.
        _backingStore = File.createTempFile("swiftlydocument", ".basefile");
        _backingStore.deleteOnExit();

        textBuffer = new StringBuffer();
        fileOutput = new FileOutputStream(_backingStore);
        
        while ((len = data.read(buf)) > 0) {
            // Write to our base file backing
            fileOutput.write(buf, 0, len);

            // Write to the memory buffer too, oh boy
            textBuffer.append(new String(buf, 0, len, encoding));
        }

        _text = textBuffer.toString();
        _encoding = encoding;
    }

    public String getText ()
    {
        return _text;
    }

    public void setText (String text)
    {
        _text = text;
    }

    public Comparable getKey ()
    {
        return elementId;
    }

    public PathElement getPathElement ()
    {
        return _path;
    }

    public String getTextEncoding ()
    {
        return _encoding;
    }

    /** Returns an stream corresponding to the unmodified data. */
    public InputStream getOriginalData ()
        throws IOException
    {
        return new FileInputStream(_backingStore);
    }

    /** Returns an InputStream corresponding to the modified data. */
    public InputStream getModifiedData ()
        throws IOException
    {
        return new ByteArrayInputStream(_text.getBytes(_encoding));
    }

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

    /** Be sure to delete our backing store. */
    protected void finalize ()
        throws Throwable
    {
        try {
            if (_backingStore != null) {
                _backingStore.delete();                
            }
        } finally {
            super.finalize();
        }
    }

    /** Document contents, ineffeciently stored entirely in memory. */
    protected String _text;

    /** Unmodified disk-backing of the document data. */
    protected transient File _backingStore = null;

    /** Text encoding. */
    protected transient String _encoding;

    /** Reference to our associated path element. */
    protected transient PathElement _path = null;

    /** Key for the associated PathElement, used to re-bind the transient _path instance variable
     * post-serialization. */
    private Comparable _pathKey = null;
}
