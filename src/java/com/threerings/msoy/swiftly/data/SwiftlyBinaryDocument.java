//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.ImageIcon;

import org.apache.commons.io.IOUtils;

import com.threerings.msoy.swiftly.client.controller.SwiftlyDocumentEditor;
import com.threerings.msoy.swiftly.client.view.PositionLocation;

/**
 * Represents a binary file in a project and contains the data of the file.
 * Each document retains an unmodified copy of its initial data to allow for delta generation.
 */
public class SwiftlyBinaryDocument extends SwiftlyDocument
{
    public static class DocumentFactory
        implements SwiftlyDocument.DocumentFactory
    {
        public boolean handlesMimeType (String mimeType) {
            // This binary document type can handle any mimetype
            return true;
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
            return new SwiftlyBinaryDocument(path);
        }

        public SwiftlyDocument createDocument (InputStream data, PathElement path, String encoding)
            throws IOException
        {
            return new SwiftlyBinaryDocument(data, path);
        }

        /** The path to the icon for a binary document */
        private static final String DOCUMENT_ICON = "/rsrc/icons/swiftly/binary_document.png";
    }

    /** Required for the dobj system. Do not use. */
    public SwiftlyBinaryDocument ()
    {
    }

    /** Instantiate a new, blank binary document with the given path. */
    public SwiftlyBinaryDocument (PathElement path)
        throws IOException
    {
        this(null, path);
    }

    /** Instantiate a binary document with the given data and path. */
    public SwiftlyBinaryDocument (InputStream data, PathElement path)
        throws IOException
    {
        super(data, path);

        // create our blank modified data file
        _modifiedStore = File.createTempFile("swiftlydocument", ".modfile");
        _modifiedStore.deleteOnExit();

        // Copy our data into a backing store file
        if (data != null) {
            FileOutputStream fileOutput = new FileOutputStream(_backingStore);
            try {
                IOUtils.copy(data, new FileOutputStream(_backingStore));
            } finally {
                fileOutput.close();
            }
        }
    }

    @Override // from SwiftlyDocument
    public void setData (InputStream data, String encoding)
        throws IOException
    {
        FileOutputStream fileOutput = new FileOutputStream(_modifiedStore);
        try {
            IOUtils.copy(data, new FileOutputStream(_modifiedStore));
            // only set changed true if the copy worked
            _changed = true;
        } finally {
            fileOutput.close();
        }
    }

    @Override // from SwiftlyDocument
    public boolean isDirty ()
        throws IOException
    {
        // if the file was modified, perform the expensive compare
        if (_changed) {
            return !IOUtils.contentEquals(getOriginalData(), getModifiedData());
        }
        return false;
    }

    @Override // from SwiftlyDocument
    public void commit ()
        throws IOException
    {
        super.commit();

        // now that we're committed we can clear our modified store
        FileOutputStream stream = new FileOutputStream(_modifiedStore, false);
        stream.close();

        // note that we're no longer modified
        _changed = false;
    }

    @Override // from SwiftlyDocument
    public void loadInEditor (SwiftlyDocumentEditor editor, PositionLocation location)
    {
        // Cannot be displayed or edited
        return;
    }

    @Override // from SwiftlyDocument
    public InputStream getModifiedData ()
        throws IOException
    {
        return new FileInputStream(_modifiedStore);
    }

    @Override // from Object
    protected void finalize ()
        throws Throwable
    {
        // be sure to delete our modified store
        try {
            _modifiedStore.delete();
        } finally {
            super.finalize();
        }
    }

    /** If this document has been modified. */
    protected boolean _changed = false;

    /** Modified disk-backing of the document data or null. */
    protected transient File _modifiedStore;
}
