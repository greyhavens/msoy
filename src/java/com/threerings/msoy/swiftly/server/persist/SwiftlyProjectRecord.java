//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import java.sql.Timestamp;
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.swiftly.data.all.SwiftlyProject;

/**
 * Contains the definition of a swiftly project.
 */
@Entity(uniqueConstraints={
    @UniqueConstraint(name="ownerProject", fields={ "ownerId", "projectName" })
})
public class SwiftlyProjectRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SwiftlyProjectRecord> _R = SwiftlyProjectRecord.class;
    public static final ColumnExp PROJECT_ID = colexp(_R, "projectId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp PROJECT_TYPE = colexp(_R, "projectType");
    public static final ColumnExp CREATION_DATE = colexp(_R, "creationDate");
    public static final ColumnExp PROJECT_NAME = colexp(_R, "projectName");
    public static final ColumnExp STORAGE_ID = colexp(_R, "storageId");
    public static final ColumnExp REMIXABLE = colexp(_R, "remixable");
    public static final ColumnExp DELETED = colexp(_R, "deleted");
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

    public Map<ColumnExp, Object> findUpdates (SwiftlyProject project)
    {
        Map<ColumnExp, Object> updates = Maps.newHashMap();
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
     * Create and return a primary {@link Key} to identify a {@link SwiftlyProjectRecord}
     * with the supplied key values.
     */
    public static Key<SwiftlyProjectRecord> getKey (int projectId)
    {
        return new Key<SwiftlyProjectRecord>(
                SwiftlyProjectRecord.class,
                new ColumnExp[] { PROJECT_ID },
                new Comparable[] { projectId });
    }
    // AUTO-GENERATED: METHODS END
}
