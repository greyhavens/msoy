//
// $Id$

package com.threerings.msoy.money.server.persist;

/**
 * Generic exception indicating a problem has occurred accessing a repository.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
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
