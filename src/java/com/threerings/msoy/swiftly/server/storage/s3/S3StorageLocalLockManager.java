//$Id $
//

package com.threerings.msoy.swiftly.server.storage.s3;

import java.util.Date;
import java.util.Map;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.Maps;

/**
 * Local-only S3 lock management.
 * 
 * This lock manager can only guarantee storage consistency insofar as ALL
 * storage accessors use the same S3StorageLocalLockManager instance.
 * 
 * This implementation does not do any external communication or synchronization, and
 * IS NOT SUITABLE for handling locking across multiple servers, or even between
 * multiple instances of the S3StorageLocalLockManager.
 */
public class S3StorageLocalLockManager implements S3StorageLockManager {
    
    public S3ObjectLock lockObject(Object owner, String objectKey, int timeout)
        throws StorageLockUnavailableException
    {
        LockEntry entry = null;
        final long startTime;
        long waitTime;
        
        // Acquire the objectTable lock
        _lock.lock();
        
        // Mark the start time, initialize the waitTime (we'll decrement this as time elapses)
        startTime = new Date().getTime();
        waitTime = timeout;
        
        // If the key exists, we need to wait for it to be unlocked and removed. Loop
        // until either our timeout expires (and we throw an exception), or we can
        // safely lock objectKey
        while (true) {
            // Retrieve the entry, if it exists
            entry = _objectTable.get(objectKey);
            
            if (entry == null) {
                // We're done. The entry doesn't exist and we hold a lock on the object
                // table. break out of the loop so we can add our lock.
                break;
            }
            
            // Someone else holds the lock, so let's wait for the unlock event. This
            // releases our lock on the table -- we'll hold the lock again when it returns.
            while (waitTime > 0) {
                try {
                    // Wait for lock removal notification
                    if (entry.getCondition().await(timeout, TimeUnit.SECONDS) == false) {
                        // We don't hold the lock, so we can safely abort here.
                        throw new StorageLockUnavailableException("Timeout exceeded waiting for lock");
                    }
                    
                    // Notified, and we hold the lock. Break out of our condition-waiting loop.
                    break;
                    
                } catch (InterruptedException e) {
                    // We're interrupted, and we don't hold the lock. Update the waitTime according to
                    // elapsed time, and check if we should continue
                    final long curTime = new Date().getTime();
                    final long timeDiff  = TimeUnit.SECONDS.convert(curTime - startTime, TimeUnit.MILLISECONDS);
                    waitTime -= timeDiff;
                    
                    // Timeout exceeded
                    if (waitTime <= 0)
                        throw new StorageLockUnavailableException("Timeout exceeded waiting for lock");
                    
                    // Timeout not exceeded, re-assert our lock
                    _lock.lock();
                }
            }
            
            // If we got here, we hold a lock on the object table, and we've been notified that the object lock
            // has been released for our objectKey. It's possible that someone else has re-asserted a lock
            // for the key, so we let the loop run again to ensure that a new lock hasn't been added
        }
        
        // Once we're here, we are assured that:
        //     - We hold the table lock
        //     - No lock on our object exists
        // The only thing left to do is add our lock to the table, unlock the table, and return.
        final LockEntry newLockEntry = new LockEntry(this, owner, objectKey, _lock.newCondition());
        _objectTable.put(objectKey, newLockEntry);
        
        // Release the objectTable lock
        _lock.unlock();
        
        return newLockEntry;
    }
    
    private void unlockObject(Object owner, LockEntry entry) {
        // Acquire a lock on our object table
        _lock.lock();
        
        // Remove our entry lock from the table
        _objectTable.remove(entry.getObjectKey());
        
        // Notify all entry listeners
        entry.getCondition().signalAll();
        
        // Release our object table lock
        _lock.unlock();
    }
    
    /** A single lock entry */
    static class LockEntry implements S3StorageLockManager.S3ObjectLock {
        LockEntry (S3StorageLocalLockManager lockMgr, Object owner,
                String objectKey, Condition unlockCondition) {
            _lockMgr = lockMgr;
            _owner = owner;
            _objectKey = objectKey;
            _cond = unlockCondition;
        }
        
        public void unlock () {
            _lockMgr.unlockObject(_owner, this);
        }
        
        Object getOwner () {
            return _owner;
        }
        
        String getObjectKey () {
            return _objectKey;
        }
        
        Condition getCondition () {
            return _cond;
        }
        
        /** The lock manager. */
        private final S3StorageLocalLockManager _lockMgr;
        
        /** The lock owner. */
        private final Object _owner;
        
        /** The lock object key. */
        private final String _objectKey;
        
        /** The unlock condition. */
        private final Condition _cond;
    }
    
    /**
     * A single lock for controlling addition/removal of {@link LockEntry}
     * instances to the _objectLocks hash table
     */
    private final Lock _lock = new ReentrantLock();
    
    /** Hash table of object lock entries. */
    private final Map<String,LockEntry> _objectTable = Maps.newHashMap();
}
