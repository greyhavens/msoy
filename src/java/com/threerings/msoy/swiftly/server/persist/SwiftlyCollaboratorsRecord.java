//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains a mapping of member to swiftly project.
 */
@Entity
public class SwiftlyCollaboratorsRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SwiftlyCollaboratorsRecord> _R = SwiftlyCollaboratorsRecord.class;
    public static final ColumnExp PROJECT_ID = colexp(_R, "projectId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp BUILD_RESULT_ITEM_ID = colexp(_R, "buildResultItemId");
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
     * Create and return a primary {@link Key} to identify a {@link SwiftlyCollaboratorsRecord}
     * with the supplied key values.
     */
    public static Key<SwiftlyCollaboratorsRecord> getKey (int projectId, int memberId)
    {
        return new Key<SwiftlyCollaboratorsRecord>(
                SwiftlyCollaboratorsRecord.class,
                new ColumnExp[] { PROJECT_ID, MEMBER_ID },
                new Comparable[] { projectId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
