package com.threerings.msoy.swiftly.server.storage.s3;

import java.security.SecureRandom;

import com.threerings.s3.client.S3Connection;

/**
 * A simple unit test {@link S3StorageConnectionFactory}.
 * Connection configuration is derived from Java properties.
 */
public class S3UnitTestConnectionFactory
    implements S3StorageConnectionFactory
{
    /**
     * Generate a unique test bucket name.
     */
    public static String generateTestBucketName ()
    {
        int random = new SecureRandom().nextInt(Integer.MAX_VALUE);
        return  "test-" + AWS_ID + "-" + random;
    }

    public S3Connection getConnection () {
        return new S3Connection(AWS_ID, AWS_KEY);
    }

    /** AWS test ID. */
    private static final String AWS_ID = System.getProperty("test.aws.id");

    /** AWS test key. */
    private static final String AWS_KEY = System.getProperty("test.aws.key");
}
