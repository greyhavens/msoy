// $Id $
//

package com.threerings.msoy.swiftly.server.storage.s3.operation;

import junit.framework.JUnit4TestAdapter;

import org.junit.Before;
import org.junit.Test;

import com.threerings.msoy.swiftly.server.storage.s3.S3StorageLocalLockManager;
import com.threerings.msoy.swiftly.server.storage.s3.S3UnitTestConnectionFactory;
import com.threerings.s3.client.S3Connection;

public class S3InitStorageOperationUnitTest {
    @Before
    public void setUp () {
        _conn = new S3UnitTestConnectionFactory().getConnection();
    }

    @Test
    public void testInitStorage () {
        S3InitStorageOperation op = new S3InitStorageOperation(new S3StorageLocalLockManager(), _conn, _bucket, "");
    }

    /** S3 Connection. */
    private S3Connection _conn;

    /** Test bucket. */
    private String _bucket = S3UnitTestConnectionFactory.generateTestBucketName();

    /** Ant 1.6 compatibility */
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(S3InitStorageOperationUnitTest.class);
    }
}
