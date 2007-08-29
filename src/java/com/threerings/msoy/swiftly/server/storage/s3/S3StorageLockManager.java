package com.threerings.msoy.swiftly.server.storage.s3;

import java.io.File;

import com.threerings.msoy.swiftly.server.storage.ProjectS3Storage;

/**
 * S3 lock manager.
 * @author landonf
 */
public interface S3StorageLockManager
{
    public static class StorageLockUnavailableException
        extends Exception
    {
        public StorageLockUnavailableException (String message) {
            super(message);
        }
        
        public StorageLockUnavailableException (String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void lockDirectory (ProjectS3Storage storage, File directory) throws StorageLockUnavailableException;
    public void unlockDirectory (ProjectS3Storage storage, File directory);
}
