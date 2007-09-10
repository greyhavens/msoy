// $Id$
//

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.swiftly.server.storage.s3.S3StorageConnectionFactory;
import com.threerings.msoy.swiftly.server.storage.s3.S3StorageLocalLockManager;
import com.threerings.msoy.swiftly.server.storage.s3.S3UnitTestConnectionFactory;
import com.threerings.s3.client.S3Connection;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class ProjectS3StorageUnitTest {

    @Before
    public void setUp () {
        _storage = new ProjectS3Storage(ProjectStorageUnitTest.mockProject(), new S3UnitTestConnectionFactory(), new S3StorageLocalLockManager(), "testBucket");
    }


    @Test
    public void testSomething () {

    }

    /** Our test storage instance. */
    private ProjectS3Storage _storage;
}
