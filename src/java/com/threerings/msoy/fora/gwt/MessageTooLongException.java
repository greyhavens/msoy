//
// $Id$

package com.threerings.msoy.fora.gwt;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Thrown when a forum message that is too long is submitted.
 */
public class MessageTooLongException extends ServiceException
{
    /** Reports a message of the specified (sanitized) length as being too long. */
    public MessageTooLongException (int length)
    {
        // no message
        _length = length;
    }

    /** Used for unserialization. */
    public MessageTooLongException ()
    {
    }

    /**
     * Returns the length of the sanitized message.
     */
    public int getMessageLength ()
    {
        return _length;
    }

    protected int _length;
}
