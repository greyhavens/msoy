//
// $Id$

package com.threerings.msoy.swiftly.server.persist;


import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;

/**
 * Manages the persistent information associated with a member's projects.
 */
public class SwiftlyProjectRepository extends DepotRepository
{
    public SwiftlyProjectRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    /**
     * Loads the project record for the specified project. Returns null if no
     * record has been created for that project.
     */
    public SwiftlyProjectRecord loadProject (int projectId)
        throws PersistenceException
    {
        return load(SwiftlyProjectRecord.class, projectId);
    }

    /**
     * Stores the supplied project record in the database, overwriting previously
     * stored project data.
     */
    public void storeProject (SwiftlyProjectRecord record)
        throws PersistenceException
    {
        store(record);
    }
}