//
// $Id$

package com.threerings.msoy.admin.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains summary information for a particular test's group.
 */
public class ABGroupSummaryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ABGroupSummaryRecord> _R = ABGroupSummaryRecord.class;
    public static final ColumnExp TEST_ID = colexp(_R, "testId");
    public static final ColumnExp GROUP = colexp(_R, "group");
    public static final ColumnExp ASSIGNED = colexp(_R, "assigned");
    public static final ColumnExp REGISTERED = colexp(_R, "registered");
    public static final ColumnExp RETAINED = colexp(_R, "retained");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** The test to which this record pertains. */
    @Id public int testId;

    /** The group being described by this record. */
    @Id public int group;

    /** The number of visitors assigned to this group. */
    public int assigned;

    /** The number of those visitors that played. */
    public int played;

    /** The number of those visitors that registered. */
    public int registered;

    /** The number of the registrants that validated their email. */
    public int validated;

    /** The number of the registrants that returned after two days. */
    public int returned;

    /** The number of the registrants that returned after one week. */
    public int retained;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ABGroupSummaryRecord}
     * with the supplied key values.
     */
    public static Key<ABGroupSummaryRecord> getKey (int testId, int group)
    {
        return new Key<ABGroupSummaryRecord>(
                ABGroupSummaryRecord.class,
                new ColumnExp[] { TEST_ID, GROUP },
                new Comparable[] { testId, group });
    }
    // AUTO-GENERATED: METHODS END
}
