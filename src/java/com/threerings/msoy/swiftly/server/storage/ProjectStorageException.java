//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

/**
 * Thrown when a swiftly storage error occurs. Supplied messages are not
 * intended for localization or end-user consumption.
 */
public class ProjectStorageException extends Exception
{
    public ProjectStorageException (String message)
    {
        this(message, null);
    }

    public ProjectStorageException (String message, Throwable cause)
    {
        super(message, cause);
    }

    /** Internal storage-backend  error. */
    public static class InternalError extends ProjectStorageException {
        public InternalError (String message)
        {
            this(message, null);
        }

        public InternalError (String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /** API client requested something that would lead to a consistency failure. 
     * This will almost always be due to programmer error.
     */
    public static class ConsistencyError extends ProjectStorageException {
        public ConsistencyError (String message) {
            this(message, null);
        }

        public ConsistencyError (String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /** A transient failure occured. */
    public static class TransientFailure extends ProjectStorageException {
        public TransientFailure (String message) {
            this(message, null);
        }

        public TransientFailure (String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
