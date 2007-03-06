//
// $Id$

package com.threerings.msoy.swiftly.server.build;

/**
 * Thrown when a swiftly builder error occurs. Supplied messages are not
 * intended for localization or end-user consumption.
 */
public class ProjectBuilderException extends Exception
{
    public ProjectBuilderException (String message)
    {
        this(message, null);
    }

    public ProjectBuilderException (String message, Throwable cause)
    {
        super(message, cause);
    }

    /** Internal, undefined error. */
    public static class InternalError extends ProjectBuilderException {
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
