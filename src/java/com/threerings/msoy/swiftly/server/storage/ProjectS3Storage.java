// $Id $
//

package com.threerings.msoy.swiftly.server.storage;

import java.io.File;
import java.util.List;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.server.storage.s3.ProjectS3ConnectionFactory;

/**
 * Implements an S3-backed project storage repository.
 *
 * @author landonf
 */
public class ProjectS3Storage implements ProjectStorage
{

    public ProjectS3Storage (final ProjectS3ConnectionFactory connectionFactory) {
        _factory = connectionFactory;
    }

    public void deleteDocument (final PathElement pathElement, final String logMessage)
        throws ProjectStorageException
    {
        // TODO Auto-generated method stub

    }

    public void export (final File exportDirectory) throws ProjectStorageException
    {
        // TODO Auto-generated method stub

    }

    public SwiftlyDocument getDocument (final PathElement path) throws ProjectStorageException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<PathElement> getProjectTree () throws ProjectStorageException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void putDocument (final SwiftlyDocument document, final String logMessage)
        throws ProjectStorageException
    {
        // TODO Auto-generated method stub

    }

    public void renameDocument (final PathElement pathElement, final String newName, final String logMessage)
        throws ProjectStorageException
    {
        // TODO Auto-generated method stub

    }

    /** S3Connection factory. */
    private final ProjectS3ConnectionFactory _factory;
}
