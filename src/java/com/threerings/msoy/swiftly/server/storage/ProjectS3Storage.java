// $Id $
//

package com.threerings.msoy.swiftly.server.storage;

import java.io.File;
import java.util.List;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.msoy.swiftly.server.storage.s3.S3StorageConnectionFactory;
import com.threerings.msoy.swiftly.server.storage.s3.S3StorageLockManager;

/**
 * Implements an S3-backed project storage repository.
 *
 * @author landonf
 */
public class ProjectS3Storage implements ProjectStorage
{

    public ProjectS3Storage (SwiftlyProject project, S3StorageConnectionFactory connectionFactory,
    		S3StorageLockManager lockMgr, String storageBucket)
    {
// TODO
//        _factory = connectionFactory;
//        _lockMgr = lockMgr;
//        _bucket = storageBucket;
    }

    public void deleteDocument (PathElement pathElement, String logMessage)
        throws ProjectStorageException
    {
        // TODO Auto-generated method stub

    }

    public void export (File exportDirectory) throws ProjectStorageException
    {
        // TODO Auto-generated method stub

    }

    public SwiftlyDocument getDocument (PathElement path) throws ProjectStorageException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<PathElement> getProjectTree () throws ProjectStorageException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void putDocument (SwiftlyDocument document, String logMessage)
        throws ProjectStorageException
    {
        // TODO Auto-generated method stub

    }

    public void renameDocument (PathElement pathElement, String newName, String logMessage)
        throws ProjectStorageException
    {
        // TODO Auto-generated method stub

    }

    /** S3Connection factory. */
//    private final S3StorageConnectionFactory _factory;

    /** S3 Lock Manager. */
//    private final S3StorageLockManager _lockMgr;

    /** S3 storage bucket. */
//    private final String _bucket;
}
