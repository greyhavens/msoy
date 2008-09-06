//
// $Id$

package com.threerings.msoy.money.server.persist;

import com.samskivert.jdbc.depot.DatabaseException;

/**
 * Indicates an update or insert was attempted on an entity, but the entity was updated in another
 * thread since it was last loaded.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class StaleDataException extends DatabaseException
{
    public StaleDataException (String message)
    {
        super(message);
    }
}
