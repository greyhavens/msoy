package com.threerings.msoy.swiftly.server.storage.s3;

/**
 * S3 lock manager. Responsible for providing per-object locking for all
 * potentially conflicting clients, locally or within a cluster. All
 * clients must be provided coherent, synchronized access to the locking
 * service implementation -- failure to do so may result in data loss.
 *
 * WARNING: This API should be considered unstable and subject to change
 * until ample time has been spent working out impendence mismatches with
 * a distributed locking system.
 * 
 * @author landonf@threerings.net
 */
public interface S3StorageLockManager
{
	/**
	 * Exception thrown if a lock can not be acquired.
	 */
    public static class StorageLockUnavailableException extends Exception {
        public StorageLockUnavailableException (String message) {
            super(message);
        }

        public StorageLockUnavailableException (String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * S3 Object Lock.
     */
    public interface S3ObjectLock {
    }

    
    /**
     * Lock the given S3 object on behalf of the specified owner.
     * 
     * @param owner Identifying entity that will own the lock
     * @param objectKey The S3 object key to lock
     * @param timeout The maximum time to wait on the lock, in seconds.
     * @throws StorageLockUnavailableException Thrown if the lock can not be acquired within the specified period.
     */
    public S3ObjectLock lockObject (Object owner, String objectKey, int timeout) throws StorageLockUnavailableException;


    /**
     * Release the lock held by specified owner.
     * 
     * @param lock The lock to release
     */
    public void unlockObject (Object owner, S3ObjectLock lock);
}
