//
// $Id$

package com.threerings.msoy.money.server.impl;

public class RepositoryException extends RuntimeException
{
    public RepositoryException (final String msg)
    {
        super(msg);
    }
    
    public RepositoryException (final String msg, final Throwable cause)
    {
        super(msg, cause);
    }
    
    public RepositoryException (final Throwable cause)
    {
        super(cause);
    }
}
