//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Date;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.clause.Where;
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
    public SwiftlyProjectRecord loadProject (int memberId, int projectId)
        throws PersistenceException
    {
        // TODO: verify permissions on the returned project using memberId
        return load(SwiftlyProjectRecord.class, projectId);
    }

    /**
     * Stores the supplied project record in the database, overwriting previously
     * stored project data.
     */
    public SwiftlyProjectRecord createProject (int memberId, String projectName, int projectTypeId)
        throws PersistenceException
    {
        SwiftlyProjectRecord record = new SwiftlyProjectRecord();
        record.projectName = projectName;
        record.ownerId = memberId;
        record.projectTypeId = projectTypeId;
        record.creationDate = new Timestamp(System.currentTimeMillis());

        try {
            insert(record);
        } catch (DuplicateKeyException dke) {
            // TODO: Throw an exception here? If someone is expecting this to *create* a
            // repository, returning an existing one isn't likely to be what they expect.

            // ownerId,projectName already exists, return it
            return load(SwiftlyProjectRecord.class, SwiftlyProjectRecord.OWNER_ID,
                record.ownerId, SwiftlyProjectRecord.PROJECT_NAME, record.projectName);
        }
        return record;
    }

    /**
     * Gets all the project types available.
     */
    public ArrayList<SwiftlyProjectTypeRecord> getProjectTypes (int memberId)
        throws PersistenceException
    {
        // TODO: Use privileges, instead of just ownership.
        ArrayList<SwiftlyProjectTypeRecord> types = new ArrayList<SwiftlyProjectTypeRecord>();
        for (SwiftlyProjectTypeRecord record : findAll(SwiftlyProjectTypeRecord.class)) {
            types.add(record);                  
        }

        return types;
    }
}
