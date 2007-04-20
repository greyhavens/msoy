//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.threerings.msoy.swiftly.client.SwiftlyDocumentEditor;

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

        public SwiftlyDocument createDocument (InputStream data, PathElement path, String encoding)
            throws IOException
        {
            return new SwiftlyBinaryDocument(data, path);
        }
    }

    /** Required for the dobj system. Do not use. */
    protected SwiftlyBinaryDocument ()
    {
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
        } finally {
            fileOutput.close();
        }
    }

    @Override // from SwiftlyDocument
    public boolean isDirty ()
        throws IOException
    {
        if (_modifiedStore != null) {
            // if input was received, perform the expensive compare
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
    }

    @Override // from SwiftlyDocument
    public void loadInEditor (SwiftlyDocumentEditor editor)
    {
        // Cannot be displayed or edited
        return;
    }

    @Override // from SwiftlyDocument
    public InputStream getModifiedData ()
        throws IOException
    {
        return _modifiedStore == null ? null : new FileInputStream(_modifiedStore);
    }

    @Override // from Object
    protected void finalize ()
        throws Throwable
    {
        // be sure to delete our modified store
        try {
            if (_modifiedStore != null) {
                _modifiedStore.delete();
            }
        } finally {
            super.finalize();
        }
    }

    /** Modified disk-backing of the document data or null. */
    protected transient File _modifiedStore;
}
