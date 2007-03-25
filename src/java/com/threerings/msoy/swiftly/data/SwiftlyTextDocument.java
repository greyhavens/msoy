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

import com.threerings.msoy.swiftly.client.SwiftlyDocumentEditor;

/**
 * Represents a source file in a project and contains the text of the file.
 * Text is stored in the provided encoding, and each document retains an
 * unmodified copy of its initial data to allow for delta generation.
 */
public class SwiftlyTextDocument extends SwiftlyDocument
{    
    @Override // from SwiftlyDocument
    public void init (InputStream data, PathElement path, String encoding)
        throws IOException
    {
        StringBuffer textBuffer;
        FileOutputStream fileOutput;
        byte[] buf = new byte[1024];
        int len;

        // Store the pathelement
        _path = path;

        // Load and save the base document data.
        // TODO: Stack of deltas and a mmap()'d base document, such that we
        // don't waste RAM storing the whole file in memory.
        _backingStore = File.createTempFile("swiftlydocument", ".basefile");
        _backingStore.deleteOnExit();

        textBuffer = new StringBuffer();
        fileOutput = new FileOutputStream(_backingStore);
        
        // text will remain blank if this is a new document
        _text = "";
        if (data != null) {
            while ((len = data.read(buf)) > 0) {
                // Write to our base file backing
                fileOutput.write(buf, 0, len);

                // Write to the memory buffer too, oh boy
                textBuffer.append(new String(buf, 0, len, encoding));
            }
            _text = textBuffer.toString();
        }

        _encoding = encoding;
    }

    @Override // from SwiftlyDocument
    public void commit ()
        throws IOException
    {
        InputStream data = getModifiedData();
        FileOutputStream fileOutput = new FileOutputStream(_backingStore);
        byte[] buf = new byte[1024];
        int len;

        while ((len = data.read(buf)) > 0) {
            // Write to our base file backing
            fileOutput.write(buf, 0, len);
        }

        _changed = false;
    }

    public String getText ()
    {
        return _text;
    }

    public void setText (String text)
    {
        _text = text;
        _changed = true;
    }

    @Override // from SwiftlyDocument
    public boolean isDirty ()
        throws IOException
    {
        // first check to see if the document has received any user input
        if (_changed) {
            // if input was received, perform the expensive compare
            return !IOUtils.contentEquals(getOriginalData(), getModifiedData());
        }
        return false;
    }

    @Override // from SwiftlyDocument
    public void loadInEditor (SwiftlyDocumentEditor editor)
    {
        editor.editTextDocument(this);
    }

    @Override // from SwiftlyDocument
    public boolean handlesMimeType (String mimeType)
    {
        for (String type : _mimeTypes) {
            if (mimeType.startsWith(type)) {
                return true;
            }
        }
        return false;
    }

    public String getTextEncoding ()
    {
        return _encoding;
    }

    @Override // from SwiftlyDocument
    public InputStream getOriginalData ()
        throws IOException
    {
        return new FileInputStream(_backingStore);
    }

    @Override // from SwiftlyDocument
    public InputStream getModifiedData ()
        throws IOException
    {
        return new ByteArrayInputStream(_text.getBytes(_encoding));
    }

    /** Be sure to delete our backing store. */
    @Override // from Object
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

    /** If this document has received any input. */
    protected boolean _changed = false;

    /** Unmodified disk-backing of the document data. */
    protected transient File _backingStore = null;

    /** Text encoding. */
    protected transient String _encoding;

    /** Mime types supported by this document type. */
    protected String[] _mimeTypes = {"text/"};
}
