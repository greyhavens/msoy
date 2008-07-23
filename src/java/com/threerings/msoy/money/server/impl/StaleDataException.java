//
// $Id$

package com.threerings.msoy.money.server.impl;

public class StaleDataException extends Exception
{
    public StaleDataException ()
    {
        super();
    }
    
    public StaleDataException (final String message)
    {
        super(message);
    }
}
