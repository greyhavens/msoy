//
// $Id$

package com.threerings.msoy.swiftly.server.storage.s3;

import com.threerings.s3.client.S3Connection;

/**
 * Interface for providing S3Connection instances to {@link ProjectS3Storage}
 *
 * @author landonf
 */
public interface S3StorageConnectionFactory {
    /**
     * Exception thrown if a connection can not be provided.
     */
    public static class ConnectionUnavailableException extends Exception {
        public ConnectionUnavailableException (final String message) {
            super(message);
        }

        public ConnectionUnavailableException (final String message, final Throwable cause) {
            super(message, cause);
        }
    };

    public S3Connection getConnection () throws ConnectionUnavailableException;
}
