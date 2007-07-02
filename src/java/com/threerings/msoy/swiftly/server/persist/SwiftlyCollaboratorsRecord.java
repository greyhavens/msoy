//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Contains a mapping of member to swiftly project.
 */
@Entity
public class SwiftlyCollaboratorsRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #projectId} field. */
    public static final String PROJECT_ID = "projectId";

    /** The qualified column identifier for the {@link #projectId} field. */
    public static final ColumnExp PROJECT_ID_C =
        new ColumnExp(SwiftlyCollaboratorsRecord.class, PROJECT_ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(SwiftlyCollaboratorsRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #buildResultItemId} field. */
    public static final String BUILD_RESULT_ITEM_ID = "buildResultItemId";

    /** The qualified column identifier for the {@link #buildResultItemId} field. */
    public static final ColumnExp BUILD_RESULT_ITEM_ID_C =
        new ColumnExp(SwiftlyCollaboratorsRecord.class, BUILD_RESULT_ITEM_ID);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    /** The SwiftlyProject this memberId is a member of. */
    @Id public int projectId; /*TODO: this should have a foreign key constraint */

    /** The memberId of the project member. */
    @Id public int memberId; /*TODO: this should have a foreign key constraint */
    
    /** The itemId of the build result for this member, for this project. */
    public int buildResultItemId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #SwiftlyCollaboratorsRecord}
     * with the supplied key values.
     */
    public static Key<SwiftlyCollaboratorsRecord> getKey (int projectId, int memberId)
    {
        return new Key<SwiftlyCollaboratorsRecord>(
                SwiftlyCollaboratorsRecord.class,
                new String[] { PROJECT_ID, MEMBER_ID },
                new Comparable[] { projectId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
