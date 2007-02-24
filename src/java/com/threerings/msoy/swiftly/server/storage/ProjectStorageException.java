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
}
