//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.SerializableException;

/**
 * Thrown to indicate a failure during logon.
 */
public class LogonException extends SerializableException
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
