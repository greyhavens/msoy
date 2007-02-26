//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.ConnectionProvider;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.clause.Where;

/**
 * Manages the persistent information associated with a member's projects.
 */
public class SwiftlyRepository extends DepotRepository
{
    public SwiftlyRepository (ConnectionProvider conprov)
    {
        super(conprov);
        
        // XXX Initial project types -- Temporary until we sort out what behavior
        // a project actually inherits from its type, and how to organize project
        // types
        _ctx.registerMigration(SwiftlyProjectTypeRecord.class, new EntityMigration(3) {
            public int invoke (Connection conn) throws SQLException {
                Statement stmt;
                int retVal = 0;

                // The illustrious "game"
                stmt = conn.createStatement();
                try {
                    // XXX _tableName is null here, and I have no idea why.
                    retVal = stmt.executeUpdate("INSERT INTO SwiftlyProjectTypeRecord" +
                        " (typeName, displayName) VALUES ('game', 'Whirled Game');");
                } finally {
                    stmt.close();
                }

                return retVal;
            }
        });
    }

    /**
     * Find all the projects which have the remixable flag set.
     */
     // TODO: this should take a limit parameter
    public List<SwiftlyProjectRecord> findRemixableProjects ()
        throws PersistenceException
    {
        ArrayList<SwiftlyProjectRecord> projects = new ArrayList<SwiftlyProjectRecord>();
        for (SwiftlyProjectRecord record : findAll(
                SwiftlyProjectRecord.class,
                new Where(SwiftlyProjectRecord.REMIXABLE_C, true))) {
                
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
    public SwiftlyProjectRecord createProject (int memberId, String projectName, int projectTypeId,
                                               boolean remixable)
        throws PersistenceException
    {
        SwiftlyProjectRecord record = new SwiftlyProjectRecord();
        record.projectName = projectName;
        record.ownerId = memberId;
        record.projectTypeId = projectTypeId;
        // TODO:
        record.projectSubversionURL = "dummy value";
        record.remixable = remixable;
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
    public List<SwiftlyProjectTypeRecord> getProjectTypes (int memberId)
        throws PersistenceException
    {
        // TODO: Use privileges, instead of just ownership.
        ArrayList<SwiftlyProjectTypeRecord> types = new ArrayList<SwiftlyProjectTypeRecord>();
        for (SwiftlyProjectTypeRecord record : findAll(SwiftlyProjectTypeRecord.class)) {
            types.add(record);                  
        }

        return types;
    }

    /**
     * Fetches the collaborators for a given project.
     */
    public Collection<SwiftlyCollaboratorsRecord> getCollaborators (int projectId)
        throws PersistenceException
    {
        return findAll(SwiftlyCollaboratorsRecord.class,
                       new Where(SwiftlyCollaboratorsRecord.PROJECT_ID, projectId));
    }

    /**
     * Fetches the projects a given member is a collaborator on.
     */
    public Collection<SwiftlyCollaboratorsRecord> getMemberships (int memberId)
        throws PersistenceException
    {
        return findAll(SwiftlyCollaboratorsRecord.class,
                       new Where(SwiftlyCollaboratorsRecord.MEMBER_ID, memberId));
    }

    /**
     * Adds a member to the list of collaborators for a project.
     */
    public SwiftlyCollaboratorsRecord joinCollaborators (int projectId, int memberId)
        throws PersistenceException
    {
        SwiftlyCollaboratorsRecord record = new SwiftlyCollaboratorsRecord();
        record.projectId = projectId;
        record.memberId = memberId;
        insert(record);
        return record;
    }

    /**
     * Remove a given member as a collaborator on a given project. This method returns
     * false if there was no membership to cancel.
     */
    // TODO do we need to work on rows or is it safe to just remove one record?
    public boolean leaveCollaborators (int projectId, int memberId)
        throws PersistenceException
    {
        Key<SwiftlyCollaboratorsRecord> key = new Key<SwiftlyCollaboratorsRecord>(
                SwiftlyCollaboratorsRecord.class,
                SwiftlyCollaboratorsRecord.PROJECT_ID, projectId,
                SwiftlyCollaboratorsRecord.MEMBER_ID, memberId);
        int rows = deleteAll(SwiftlyCollaboratorsRecord.class, key, key);
        return rows > 0;
    }

}
