//
// $Id$

package com.threerings.msoy.swiftly.server.persist;


import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.DepotRepository;

import java.util.ArrayList;


/**
 * Manages the persistent information associated with a member's projects.
 */
public class SwiftlyProjectRepository extends DepotRepository
{
    public SwiftlyProjectRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    public ArrayList<SwiftlyProjectRecord> findProjects (int memberId)
        throws PersistenceException
    {
        // TODO: Use privileges, instead of just ownership.
        ArrayList<SwiftlyProjectRecord> projects = new ArrayList<SwiftlyProjectRecord>();
        for (SwiftlyProjectRecord record : findAll(
                SwiftlyProjectRecord.class,
                new Where(SwiftlyProjectRecord.OWNER_ID_C, memberId))) {
                
            projects.add(record);                  
        }

        return projects;
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
    public SwiftlyProjectRecord createProject (SwiftlyProjectRecord record)
        throws PersistenceException
    {
        insert(record);
        return record;
    }
}