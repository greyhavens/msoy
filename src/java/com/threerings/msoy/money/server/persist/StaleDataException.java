//
// $Id$

package com.threerings.msoy.money.server.persist;

/**
 * Indicates an update or insert was attempted on an entity, but the entity was updated in another
 * thread since it was last loaded.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class StaleDataException extends RepositoryException
{
    public StaleDataException (final String message)
    {
        super(message);
    }
}
