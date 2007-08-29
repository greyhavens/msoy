package com.threerings.msoy.swiftly.server.storage.s3;

import java.util.Date;

import com.threerings.msoy.swiftly.server.storage.ProjectS3Storage;

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
     * Exception thrown if a lock has expired.
     */
    public static class StorageLockExpiredException extends Exception {
    	public StorageLockExpiredException (String message) {
    		super(message);
    	}
    }

    
    /**
     * S3 Object Lock. Supports lock expiration, which will be used by the {@link ProjectS3Storage}
     * to help ensure lock validity and timeouts on "lost" locks.
     * 
     * It is highly recommended that the lock report an expiration date earlier than actual
     * expiration -- this will allow for a safety window in which the lock will be assumed
     * expired by the owner, prior to actual lock expiry.
     */
    public interface S3ObjectLock {
    	/**
    	 * Return the date of lock expiry, or null if the lock will not expire.
    	 * @return Date of expiration, or null if the lock will not expire.
    	 */
    	public Date getExpiration ();
    }

    
    /**
     * Lock the given S3 object on behalf of the specified owner.
     * 
     * @param owner Identifying entity that will own the lock
     * @param objectKey The S3 object key to lock
     * @param timeout The maximum time to wait on the lock.
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
