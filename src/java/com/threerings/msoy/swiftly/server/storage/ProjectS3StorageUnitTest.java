// $Id$
//

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.swiftly.server.storage.s3.ProjectS3ConnectionFactory;
import com.threerings.s3.client.S3Connection;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class ProjectS3StorageUnitTest {

    /**
     * A simple unit test S3Connection factory.
     * @author landonf
     *
     */
    public static class ConnectionFactory implements ProjectS3ConnectionFactory {
        public S3Connection getConnection () {
            return new S3Connection(AWS_ID, AWS_KEY);
        }
    }

    @Before
    public void setUp () {
        _storage = new ProjectS3Storage(new ConnectionFactory());
    }


    @Test
    public void testSomething () {

    }

    /** Our test storage instance. */
    private ProjectS3Storage _storage;

    /** AWS test ID. Move elsewhere when needed by another class. */
    private static final String AWS_ID = System.getProperty("test.aws.id");

    /** AWS test key. Move elsewhere when needed by another class. */
    private static final String AWS_KEY = System.getProperty("test.aws.key");
}
