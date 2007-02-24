//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Contains a mapping of member to swiftly project.
 */
@Entity
// @Table(uniqueConstraints = {@UniqueConstraint(columnNames={"typeName"})})
public class SwiftlyCollaboratorsRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(SwiftlyCollaboratorsRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #projectId} field. */
    public static final String PROJECT_ID = "projectId";

    /** The qualified column identifier for the {@link #projectId} field. */
    public static final ColumnExp PROJECT_ID_C =
        new ColumnExp(SwiftlyCollaboratorsRecord.class, PROJECT_ID);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The memberId of the project member. */
    public int memberId;

    /** The SwiftlyProject this memberId is a member of. */
    public int projectId;
}
