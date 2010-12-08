//
// $Id$

package com.threerings.msoy.admin.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Notes a visitor's A/B test group assignment for a particular test.
 */
public class ABGroupRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ABGroupRecord> _R = ABGroupRecord.class;
    public static final ColumnExp<Integer> TEST_ID = colexp(_R, "testId");
    public static final ColumnExp<String> VISITOR_ID = colexp(_R, "visitorId");
    public static final ColumnExp<Integer> GROUP = colexp(_R, "group");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The test for which this assignment holds. */
    @Id public int testId;

    /** The visitor in question. */
    @Id public String visitorId;

    /** The group to which the visitor was assigned. */
    public int group;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ABGroupRecord}
     * with the supplied key values.
     */
    public static Key<ABGroupRecord> getKey (int testId, String visitorId)
    {
        return newKey(_R, testId, visitorId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TEST_ID, VISITOR_ID); }
    // AUTO-GENERATED: METHODS END
}
