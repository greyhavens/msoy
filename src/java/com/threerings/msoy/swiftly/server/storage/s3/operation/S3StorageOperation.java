// $Id $
//

package com.threerings.msoy.swiftly.server.storage.s3.operation;

import com.threerings.msoy.swiftly.server.storage.s3.S3StorageLockManager;
import com.threerings.s3.client.S3Connection;

/**
 * Abstract class that provides a re-usable constructor and accessors for common
 * state required of a S3 storage operation.
 */
abstract class S3StorageOperation {
    /**
     * Create a new storage operation instance.
     * 
     * @param lockManager Shared S3 lock manager.
     * @param connection Connection used to execute the operation.
     * @param bucket S3 bucket.
     * @param repositoryPath Path to the storage repository.
     */
    public S3StorageOperation (S3StorageLockManager lockManager, S3Connection connection, String bucket, String repositoryPath) {
        _lockManager = lockManager;
        _connection = connection;
        _bucket = bucket;
        _repositoryPath = repositoryPath;
    }


    /**
     * @return The operation's S3StorageLockManager.
     */
    protected S3StorageLockManager getLockManager () {
        return _lockManager;
    }

    
    /**
     * @return The operation's S3Connection.
     */
    protected S3Connection getConnection() {
        return _connection;
    }

    /**
     * @return The operation's S3 bucket.
     */
    protected String getBucket() {
        return _bucket;
    }

    /**
     * @return The operation's repository path.
     */
    protected String getRepositoryPath () {
        return _repositoryPath;
    }

    /** S3 lock manager. */
    private final S3StorageLockManager _lockManager;
    
    /** S3 connection. */
    private final S3Connection _connection;

    /** S3 storage bucket. */
    private final String _bucket;

    /** Bucket-relative path to the repository. */
    private final String _repositoryPath;    
}
