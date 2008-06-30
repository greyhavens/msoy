//
// $Id$

package com.threerings.msoy.web.data;

/**
 * Thrown to indicate a failure during logon.
 */
public class LogonException extends Exception
{
    /**
     * Creates a logon exception with the supplied translatable string
     * indicating the cause of the failure.
     */
    public LogonException (String reason)
    {
        super(reason);
    }

    /** Used when unserializing. */
    public LogonException ()
    {
    }
}
