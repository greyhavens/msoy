//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Logic.And;
import com.threerings.msoy.server.persist.MemberRecord;

/**
 * Manages the persistent information associated with a member's projects.
 */
public class SwiftlyRepository extends DepotRepository
{
    public SwiftlyRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Find all the projects which have the remixable flag set and not the deleted flag.
     * TODO: this should take a limit parameter
     */
    public List<SwiftlyProjectRecord> findRemixableProjects ()
        throws PersistenceException
    {
        return findAll(SwiftlyProjectRecord.class,
            new Where(new And(new Equals(SwiftlyProjectRecord.REMIXABLE_C, true),
                new Equals(SwiftlyProjectRecord.DELETED_C, false))),
                OrderBy.descending(SwiftlyProjectRecord.CREATION_DATE_C));
    }

    /**
     * Find all the projects which have this member as a collaborator and not the deleted flag.
     * TODO: this should take a limit parameter
     */
    public List<SwiftlyProjectRecord> findMembersProjects (final int memberId)
        throws PersistenceException
    {
        return findAll(SwiftlyProjectRecord.class,
            new Join(SwiftlyProjectRecord.PROJECT_ID_C, SwiftlyCollaboratorsRecord.PROJECT_ID_C),
                new Where(new And(
                    new Equals(SwiftlyCollaboratorsRecord.MEMBER_ID_C, memberId),
                    new Equals(SwiftlyProjectRecord.DELETED_C, false))),
                    OrderBy.descending(SwiftlyProjectRecord.CREATION_DATE_C));
    }

    /**
     * Loads the project record for the specified project. Returns null if no record has been
     * created for that project.
     */
    public SwiftlyProjectRecord loadProject (int projectId)
        throws PersistenceException
    {
        return load(SwiftlyProjectRecord.class, SwiftlyProjectRecord.getKey(projectId));
    }

    /**
     * Loads the collaborator record for the specified project and member. Returns null if no
     * record has been created for that project and member.
     */
    public SwiftlyCollaboratorsRecord loadCollaborator (int projectId, int memberId)
        throws PersistenceException
    {
        Key<SwiftlyCollaboratorsRecord> key =
            SwiftlyCollaboratorsRecord.getKey(projectId, memberId);
        return load(SwiftlyCollaboratorsRecord.class, key);
    }

    /**
     * Deletes a project by setting the deleted flag to true, but not actually removing the record
     * from the database.
     */
    public void markProjectDeleted (int projectId)
        throws PersistenceException
    {
        SwiftlyProjectRecord project = loadProject(projectId);
        if (project == null) {
            throw new PersistenceException(
                "Couldn't find swiftly project for deletion marking. [id=" + projectId + "]");
        }
        // TODO: how do we actually want delete to work? If we decide this is the way, then
        // we should probably add a creatorId field that never changes
        project.deleted = true;
        project.ownerId = 0;
        update(project, SwiftlyProjectRecord.DELETED, SwiftlyProjectRecord.OWNER_ID);
    }

    /**
     * Deletes the specified project record.
     */
    public void deleteProject (SwiftlyProjectRecord record)
        throws PersistenceException
    {
        // delete all this project collaborators
        deleteAll(SwiftlyCollaboratorsRecord.class,
            new Where(SwiftlyCollaboratorsRecord.PROJECT_ID_C, record.projectId), null);
        delete(record);
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
        record.remixable = remixable;
        record.creationDate = new Timestamp(System.currentTimeMillis());

        insert(record);
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
     * Returns true if the memberId is a collaborator, false otherwise.
     *
     */
    public boolean isCollaborator (int projectId, int memberId)
        throws PersistenceException
    {
        return (getMembership(projectId, memberId) != null);
    }

    /**
     * Load the Swiftly SVN Storage record for the given project
     */
    public SwiftlySVNStorageRecord loadStorageRecordForProject (int projectId)
        throws PersistenceException
    {
        SwiftlyProjectRecord project = loadProject(projectId);
        // we found no ProjectRecord, we are unable to locate a StorageRecord
        if (project == null) {
            return null;
        }
        return load(SwiftlySVNStorageRecord.class,
            SwiftlySVNStorageRecord.getKey(project.storageId));
    }


    /**
     * Creates and loads a SwiftlySVNStorageRecord. If the record already exists, the existing
     * record will be returned and the database will not be modified.
     */
    public SwiftlySVNStorageRecord createSVNStorage (String protocol, String host, int port,
                                                     String baseDir)
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
            List<SwiftlySVNStorageRecord> result;

            result = findAll(SwiftlySVNStorageRecord.class,
                new Where(
                    new And(
                        new Equals(SwiftlySVNStorageRecord.PROTOCOL_C, protocol),
                        new Equals(SwiftlySVNStorageRecord.HOST_C, host),
                        new Equals(SwiftlySVNStorageRecord.PORT_C, port),
                        new Equals(SwiftlySVNStorageRecord.BASE_DIR_C, baseDir)
                    )
                )
            );

            // There can only be one!
            assert(result.size() == 1);
            return result.get(0);
        }
        return record;
    }

    /**
     * Fetches the owner MemberRecord for a given project.
     */
    public MemberRecord loadProjectOwner (int projectId)
        throws PersistenceException
    {
        return load(MemberRecord.class,
            new Join(MemberRecord.MEMBER_ID_C,
                     SwiftlyProjectRecord.OWNER_ID_C),
            new Where(new Equals(SwiftlyProjectRecord.PROJECT_ID_C, projectId)));
    }

    /**
     * Fetches the collaborators for a given project.
     */
    // TODO: sort these in a predictable manner
    public List<MemberRecord> getCollaborators (int projectId)
        throws PersistenceException
    {
        return findAll(MemberRecord.class,
            new Join(MemberRecord.MEMBER_ID_C,
                     SwiftlyCollaboratorsRecord.MEMBER_ID_C),
            new Where(new Equals(SwiftlyCollaboratorsRecord.PROJECT_ID_C, projectId)));
    }

    /**
     * Fetches the projects a given member is a collaborator on.
     */
    public List<SwiftlyCollaboratorsRecord> getMemberships (int memberId)
        throws PersistenceException
    {
        return findAll(SwiftlyCollaboratorsRecord.class,
                       new Where(SwiftlyCollaboratorsRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Fetches the membership details for a given project and member, or null.
     */
    public SwiftlyCollaboratorsRecord getMembership (int projectId, int memberId)
        throws PersistenceException
    {
        return load(SwiftlyCollaboratorsRecord.class,
                    SwiftlyCollaboratorsRecord.getKey(projectId, memberId));
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
        Key<SwiftlyCollaboratorsRecord> key =
            SwiftlyCollaboratorsRecord.getKey(projectId, memberId);
        return deleteAll(SwiftlyCollaboratorsRecord.class, key, key) > 0;
    }

    /**
     * Updates the buildResultItemId for a SwiftlyCollaboratorRecord.
     */
    public void updateBuildResultItem (int projectId, int memberId, int buildResultItemId)
        throws PersistenceException
    {
        Key<SwiftlyCollaboratorsRecord> key =
            SwiftlyCollaboratorsRecord.getKey(projectId, memberId);
        int rows = updatePartial(SwiftlyCollaboratorsRecord.class, key, key,
            SwiftlyCollaboratorsRecord.BUILD_RESULT_ITEM_ID, buildResultItemId);
        if (rows == 0) {
            throw new PersistenceException(
                "Couldn't find swiftly collaborator record for build result update [projectId=" +
                    projectId + "memberId=" + memberId + "]");
        }
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SwiftlyProjectRecord.class);
        classes.add(SwiftlySVNStorageRecord.class);
        classes.add(SwiftlyCollaboratorsRecord.class);
    }
}
