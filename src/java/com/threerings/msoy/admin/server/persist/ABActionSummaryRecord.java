//
// $Id$

package com.threerings.msoy.admin.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains summary information for one action in an A/B test.
 */
public class ABActionSummaryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ABActionSummaryRecord> _R = ABActionSummaryRecord.class;
    public static final ColumnExp TEST_ID = colexp(_R, "testId");
    public static final ColumnExp GROUP = colexp(_R, "group");
    public static final ColumnExp ACTION = colexp(_R, "action");
    public static final ColumnExp TAKERS = colexp(_R, "takers");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The test to which this record pertains. */
    @Id public int testId;

    /** The group being described by this record. */
    @Id public int group;

    /** The action being described by this record. */
    @Id public String action;

    /** The number of unique visitors that took this action. */
    public int takers;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ABActionSummaryRecord}
     * with the supplied key values.
     */
    public static Key<ABActionSummaryRecord> getKey (int testId, int group, String action)
    {
        return newKey(_R, testId, group, action);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TEST_ID, GROUP, ACTION); }
    // AUTO-GENERATED: METHODS END
}
