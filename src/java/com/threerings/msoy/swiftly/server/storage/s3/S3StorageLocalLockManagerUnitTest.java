// $Id: $
//

package com.threerings.msoy.swiftly.server.storage.s3;

import org.junit.Before;
import org.junit.Test;

import com.threerings.msoy.swiftly.server.storage.s3.S3StorageLockManager.S3ObjectLock;

import static org.junit.Assert.*;

public class S3StorageLocalLockManagerUnitTest {
    @Before
    public void setUp () {
        _lockMgr = new S3StorageLocalLockManager();
    }

    @Test
    public void testLock ()
        throws Exception
    {
        S3ObjectLock lock;

        // Make sure we unlock properly
        lock = _lockMgr.lockObject(this, "test", 20);
        lock.unlock();

        lock = _lockMgr.lockObject(this, "test", 20);
        lock.unlock();
    }

    @Test(expected=S3StorageLockManager.StorageLockUnavailableException.class)
    public void testLockTimeout ()
        throws Exception
    {
        final S3ObjectLock lock = _lockMgr.lockObject(this, "test", 20);
        
        try {
            // This lock will timeout
            _lockMgr.lockObject(this, "test", 1);
        } finally {
            lock.unlock();
        }
    }

    private S3StorageLocalLockManager _lockMgr;
}
