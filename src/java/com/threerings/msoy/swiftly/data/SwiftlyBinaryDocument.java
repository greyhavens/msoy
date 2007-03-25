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
 * Represents a binary file in a project and contains the data of the file.
 * Each document retains an unmodified copy of its initial data to allow for delta generation.
 */
public class SwiftlyBinaryDocument extends SwiftlyDocument
{
    @Override // from SwiftlyDocument
    public void init (InputStream data, PathElement path, String encoding, boolean fromRepo)
        throws IOException
    {
        // Keep track of important things
        _path = path;
        _fromRepo = fromRepo;

        // Load and save the base document data
        _backingStore = File.createTempFile("swiftlydocument", ".basefile");
        _backingStore.deleteOnExit();

        // Copy our data into a backing store file
        FileOutputStream fileOutput = new FileOutputStream(_backingStore);
        try {
            IOUtils.copy(data, new FileOutputStream(_backingStore));
        } finally {
            fileOutput.close();
        }
    }

    @Override // from SwiftlyDocument
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

    @Override // from SwiftlyDocument
    public boolean isDirty ()
        throws IOException
    {
        // if we're not from the repo, we're dirty; TODO: allow modifications, store them somewhere
        return !_fromRepo;
    }

    @Override // from SwiftlyDocument
    public void loadInEditor (SwiftlyDocumentEditor editor)
    {
        // Cannot be displayed or edited
        return;
    }

    @Override // from SwiftlyDocument
    public boolean handlesMimeType (String mimeType)
    {
        // this binary document type can handle any mimetype
        return true;
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
        // TODO
        return new FileInputStream(_backingStore);
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

    /** Unmodified disk-backing of the document data. */
    protected transient File _backingStore = null;

    /** Whether this document was created from the repository or is a brand nubian. */
    protected transient boolean _fromRepo;
}
