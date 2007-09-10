// $Id $
//

package com.threerings.msoy.swiftly.server.storage.s3.operation;

import java.io.File;

import com.threerings.msoy.swiftly.server.storage.s3.S3StorageLockManager;

import com.threerings.s3.client.S3Connection;

/**
 * S3 Storage Repository Initialization.
 */
public class S3InitStorageOperation extends S3StorageOperation {
    /**
     * Initialize a new storage repository initialization operation.
     * 
     * @param lockManager Shared S3 lock manager.
     * @param connection Connection used to execute the operation.
     * @param bucket S3 bucket.
     * @param repositoryPath Path to the storage repository.
     */
    public S3InitStorageOperation (S3StorageLockManager lockManager, S3Connection connection, String bucket, String repositoryPath) {
        super(lockManager, connection, bucket, repositoryPath);
    }

    public void initialize (File sourcePath) {
        // TODO
    }
}
