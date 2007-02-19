//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Contains the configuration of a particular member's person page.
 */
@Entity
@Table(uniqueConstraints =
       {@UniqueConstraint(columnNames={"ownerId", "projectName"})})
public class SwiftlyProjectRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 2;
    
    public static final String PROJECT_ID = "projectId";
    public static final ColumnExp PROJECT_ID_C =
        new ColumnExp(SwiftlyProjectRecord.class, PROJECT_ID);

    public static final String OWNER_ID = "ownerId";
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(SwiftlyProjectRecord.class, OWNER_ID);

    public static final String PROJECT_NAME = "projectName";
    public static final ColumnExp PROJECT_NAME_C =
        new ColumnExp(SwiftlyProjectRecord.class, PROJECT_NAME);

    /** The id of the project. */
    @Id
    @GeneratedValue
    public int projectId;
    
    /** The id of the project owner. */
    public int ownerId;

    /** The project name. */
    public String projectName;

    /** 
     * Converts this persistent record to a runtime record.
     */
    public SwiftlyProject toSwiftlyProject ()
    {
        SwiftlyProject sp = new SwiftlyProject();
        sp.projectId = projectId;
        sp.ownerId = ownerId;
        sp.projectName = projectName;
        return sp;
    }
}
