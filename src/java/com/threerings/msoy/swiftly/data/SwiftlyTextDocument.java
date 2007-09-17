//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.ImageIcon;

import org.apache.commons.io.IOUtils;

import com.threerings.msoy.swiftly.client.controller.SwiftlyDocumentEditor;
import com.threerings.msoy.swiftly.client.view.PositionLocation;

/**
 * Represents a source file in a project and contains the text of the file.
 * Text is stored in the provided encoding, and each document retains an
 * unmodified copy of its initial data to allow for delta generation.
 */
public class SwiftlyTextDocument extends SwiftlyDocument
{
    public static class DocumentFactory
        implements SwiftlyDocument.DocumentFactory
    {
        public boolean handlesMimeType (String mimeType) {
            for (String type : _mimeTypes) {
                if (mimeType.startsWith(type)) {
                    return true;
                }
            }
            return false;
        }

        public ImageIcon createIcon ()
            throws IOException
        {
            URL path = getClass().getResource(DOCUMENT_ICON);
            if (path == null) {
                throw new IOException("Icon path for SwiftlyDocument not found: " + DOCUMENT_ICON);
            }
            return new ImageIcon(path);
        }

        public SwiftlyDocument createDocument (PathElement path, String encoding)
            throws IOException
        {
            return new SwiftlyTextDocument(path, encoding);
        }

        public SwiftlyDocument createDocument (InputStream data, PathElement path, String encoding)
            throws IOException
        {
            return new SwiftlyTextDocument(data, path, encoding);
        }

        /** Mime types supported by this document type. */
        private final String[] _mimeTypes = {"text/"};

        /** The path to the icon for a text document */
        private static final String DOCUMENT_ICON = "/rsrc/icons/swiftly/text_document.png";
    }

    /** Required for the dobj system. Do not use. */
    public SwiftlyTextDocument ()
    {
    }

    /** Instantiate a new, blank text document with the given path, and text encoding. */
    public SwiftlyTextDocument (PathElement path, String encoding)
        throws IOException
    {
        this(null, path, encoding);
    }

    /** Instantiate a text document with the given data, path, and text encoding. */
    public SwiftlyTextDocument (InputStream data, PathElement path, String encoding)
        throws IOException
    {
        super(data, path);

        // TODO: Stack of deltas and a mmap()'d base document, such that we
        // don't waste RAM storing the whole file in memory.

        // text will remain blank if this is a new document
        _text = "";
        _encoding = encoding;

        if (data != null) {
            StringBuffer textBuffer = new StringBuffer();
            FileOutputStream fileOutput = new FileOutputStream(_backingStore);
            byte[] buf = new byte[1024];
            int len;
            while ((len = data.read(buf)) > 0) {
                // Write to our base file backing
                fileOutput.write(buf, 0, len);
                // Write to the memory buffer too, oh boy
                textBuffer.append(new String(buf, 0, len, encoding));
            }
            _text = textBuffer.toString();
            fileOutput.close();
        }
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
    public void setData (InputStream data, String encoding)
        throws IOException
    {
        StringBuffer textBuffer = new StringBuffer();
        byte[] buf = new byte[1024];
        int len;
        while ((len = data.read(buf)) > 0) {
            // Write to the memory buffer too, oh boy
            textBuffer.append(new String(buf, 0, len, encoding));
        }
        setText(textBuffer.toString());
    }

    @Override // from SwiftlyDocument
    public void commit ()
        throws IOException
    {
        super.commit();
        // note that we're no longer modified
        _changed = false;
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
    public void loadInEditor (SwiftlyDocumentEditor editor, PositionLocation location)
    {
        editor.editTextDocument(this, location);
    }

    public String getTextEncoding ()
    {
        return _encoding;
    }

    /**
     * Returns true if the contents of this document are the same as the supplied document.
     */
    public boolean contentsEqual (SwiftlyTextDocument other)
    {
        return getText().equals(other.getText());
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

    /** Document contents, inefficiently stored entirely in memory. */
    private String _text;

    /** If this document has received any input. */
    private boolean _changed = false;

    /** Text encoding. */
    private transient String _encoding;
}
