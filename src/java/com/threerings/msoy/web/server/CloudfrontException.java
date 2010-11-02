//
// $Id: $

package com.threerings.msoy.web.server;

public class CloudfrontException extends Exception
{
    public CloudfrontException (String message)
    {
        super(message);
    }

    public CloudfrontException (String message, Throwable cause)
    {
        super(message, cause);
    }
}
