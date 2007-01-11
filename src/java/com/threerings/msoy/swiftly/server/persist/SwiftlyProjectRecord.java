//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Contains the configuration of a particular member's person page.
 */
@Entity
@Table(uniqueConstraints =
       {@UniqueConstraint(columnNames={"ownerId", "projectName"})})
public class SwiftlyProjectRecord
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String PROJECT_ID = "projectId";
    public static final ColumnExp PROJECT_ID_C =
        new ColumnExp(SwiftlyProjectRecord.class, PROJECT_ID);

    public static final String OWNER_ID = "ownerId";
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(SwiftlyProjectRecord.class, OWNER_ID);


    /** The id of the project. */
    @Id
    @GeneratedValue
    public int projectId;
    
    /** The id of the project owner. */
    // @NotNull
    public int ownerId;

    /** The project name. */
    // @NotNull
    public String projectName;
}
