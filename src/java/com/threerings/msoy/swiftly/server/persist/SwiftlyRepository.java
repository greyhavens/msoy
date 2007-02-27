//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    public SwiftlyProjectRecord createProject (int memberId, String projectName, byte projectType,
        int storageId, boolean remixable)
        throws PersistenceException
    {
        SwiftlyProjectRecord record = new SwiftlyProjectRecord();
        record.projectName = projectName;
        record.ownerId = memberId;
        record.projectType = projectType;
        record.storageId = storageId;
        // TODO:
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
     * Updates the specified project record with supplied field/value mapping.
     */
    public void updateProject (int projectId, Map<String, Object> updates)
        throws PersistenceException
    {
        int rows = updatePartial(SwiftlyProjectRecord.class, projectId, updates);
        if (rows == 0) {
            throw new PersistenceException(
                "Couldn't find swiftly project for update [id=" + projectId + "]");
        }
    }

    /*
     * Deletes the specified project record.
     */
    public void deleteProject (SwiftlyProjectRecord record)
        throws PersistenceException
    {
        delete(record);
    }

    /**
     * Returns true if the memberId is the owner, false otherwise. 
     * 
     */
    public boolean isOwner (int projectId, int memberId)
        throws PersistenceException
    {
        SwiftlyProjectRecord project = loadProject(projectId);
        if (project == null) {
            return false;
        }
        return (project.ownerId == memberId);
    }

    /**
     * Load the Swiftly SVN Storage record for the given project
     */
    public SwiftlySVNStorageRecord loadStorageRecordForProject (int projectId)
        throws PersistenceException
    {
        SwiftlyProjectRecord project = loadProject(projectId);
        return load (SwiftlySVNStorageRecord.class, project.storageId);
    }


    /**
     * Creates and loads a SwiftlySVNStorageRecord. If the record already exists, the existing
     * record will be returned and the database will not be modified.
     */
    public SwiftlySVNStorageRecord createSVNStorage (String protocol, String host, int port, String baseDir)
        throws PersistenceException
    {
        SwiftlySVNStorageRecord record = new SwiftlySVNStorageRecord();
        record.protocol = protocol;
        record.host = host;
        record.port = port;
        record.baseDir = baseDir;

        try {
            insert(record);
        } catch (DuplicateKeyException dke) {
            // Already exists, return it
            Collection<SwiftlySVNStorageRecord> result;
            
            result = findAll(SwiftlySVNStorageRecord.class,
                new Where(SwiftlySVNStorageRecord.PROTOCOL, protocol),
                new Where(SwiftlySVNStorageRecord.HOST, host),
                new Where(SwiftlySVNStorageRecord.PORT, port),
                new Where(SwiftlySVNStorageRecord.BASE_DIR, baseDir));

            // There can only be one!
            assert(result.size() == 0);
            for (SwiftlySVNStorageRecord curRecord : result) {
                return curRecord;
            }
        }
        return record;
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
     * Fetches the membership details for a given project and member, or null.
     * 
     */
    public SwiftlyCollaboratorsRecord getMembership(int projectId, int memberId)
        throws PersistenceException
    {
        return load(SwiftlyCollaboratorsRecord.class,
                    SwiftlyCollaboratorsRecord.PROJECT_ID, projectId,
                    SwiftlyCollaboratorsRecord.MEMBER_ID, memberId);
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
