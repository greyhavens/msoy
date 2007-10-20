//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import java.sql.Timestamp;
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Contains the definition of a swiftly project.
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(fieldNames={"ownerId", "projectName"})})
public class SwiftlyProjectRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #projectId} field. */
    public static final String PROJECT_ID = "projectId";

    /** The qualified column identifier for the {@link #projectId} field. */
    public static final ColumnExp PROJECT_ID_C =
        new ColumnExp(SwiftlyProjectRecord.class, PROJECT_ID);

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(SwiftlyProjectRecord.class, OWNER_ID);

    /** The column identifier for the {@link #projectType} field. */
    public static final String PROJECT_TYPE = "projectType";

    /** The qualified column identifier for the {@link #projectType} field. */
    public static final ColumnExp PROJECT_TYPE_C =
        new ColumnExp(SwiftlyProjectRecord.class, PROJECT_TYPE);

    /** The column identifier for the {@link #creationDate} field. */
    public static final String CREATION_DATE = "creationDate";

    /** The qualified column identifier for the {@link #creationDate} field. */
    public static final ColumnExp CREATION_DATE_C =
        new ColumnExp(SwiftlyProjectRecord.class, CREATION_DATE);

    /** The column identifier for the {@link #projectName} field. */
    public static final String PROJECT_NAME = "projectName";

    /** The qualified column identifier for the {@link #projectName} field. */
    public static final ColumnExp PROJECT_NAME_C =
        new ColumnExp(SwiftlyProjectRecord.class, PROJECT_NAME);

    /** The column identifier for the {@link #storageId} field. */
    public static final String STORAGE_ID = "storageId";

    /** The qualified column identifier for the {@link #storageId} field. */
    public static final ColumnExp STORAGE_ID_C =
        new ColumnExp(SwiftlyProjectRecord.class, STORAGE_ID);

    /** The column identifier for the {@link #remixable} field. */
    public static final String REMIXABLE = "remixable";

    /** The qualified column identifier for the {@link #remixable} field. */
    public static final ColumnExp REMIXABLE_C =
        new ColumnExp(SwiftlyProjectRecord.class, REMIXABLE);

    /** The column identifier for the {@link #deleted} field. */
    public static final String DELETED = "deleted";

    /** The qualified column identifier for the {@link #deleted} field. */
    public static final ColumnExp DELETED_C =
        new ColumnExp(SwiftlyProjectRecord.class, DELETED);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 12;

    /** The id of the project. */
    @Id
    @GeneratedValue
    public int projectId;
    
    /** The id of the project owner. */
    public int ownerId;

    /** The project type. */
    public byte projectType;

    /** The time this project was created. */
    public Timestamp creationDate;

    /** The project name. */
    public String projectName;

    /** The id of the project subversion storage. */
    public int storageId;

    /** Whether this project is remixable. */
    public boolean remixable;

    /** Whether this project has been marked as deleted by the owner. */
    public boolean deleted;

    /**
     * Checks over the object definitions and will return a map of field, value pairs that contains
     * all of the entries that are not null, and are different from what's in this object
     * currently.
     */

    public Map<String, Object> findUpdates (SwiftlyProject project)
        throws PersistenceException
    {
        Map<String, Object> updates = Maps.newHashMap();
        if (project.projectName != null && !project.projectName.equals(projectName)) {
            updates.put(PROJECT_NAME, project.projectName);
        }
        if (project.remixable != remixable) {
            updates.put(REMIXABLE, project.remixable);
        } 
        return updates;
    }

    /** 
     * Converts this persistent record to a runtime record.
     */
    public SwiftlyProject toSwiftlyProject ()
    {
        SwiftlyProject sp = new SwiftlyProject();
        sp.projectId = projectId;
        sp.ownerId = ownerId;
        sp.projectType = projectType;
        sp.projectName = projectName;
        sp.remixable = remixable;
        return sp;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #SwiftlyProjectRecord}
     * with the supplied key values.
     */
    public static Key<SwiftlyProjectRecord> getKey (int projectId)
    {
        return new Key<SwiftlyProjectRecord>(
                SwiftlyProjectRecord.class,
                new String[] { PROJECT_ID },
                new Comparable[] { projectId });
    }
    // AUTO-GENERATED: METHODS END
}
