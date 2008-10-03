//
// $Id$

package com.threerings.msoy.avrg.client {

/**
 * Error thrown when an illegal operation is attempted from user code.
 */
public class UserError extends Error
{
    /**
     * Creates a new user error with the given message.
     */
    public function UserError (msg :String)
    {
        super(msg);
    }
}
}
