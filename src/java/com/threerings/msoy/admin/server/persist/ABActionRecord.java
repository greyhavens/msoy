//
// $Id$

package com.threerings.msoy.admin.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.admin.gwt.ABTest;

/**
 * Notes an action taken in a test by a participant.
 */
public class ABActionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ABActionRecord> _R = ABActionRecord.class;
    public static final ColumnExp TEST_ID = colexp(_R, "testId");
    public static final ColumnExp VISITOR_ID = colexp(_R, "visitorId");
    public static final ColumnExp ACTION = colexp(_R, "action");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The id of the test for which this action was a part. */
    @Id public int testId;

    /** The visitor that took the action. */
    @Id public String visitorId;

    /** The name of the action taken. */
    @Id @Column(length=ABTest.MAX_ACTION_LENGTH)
    public String action;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ABActionRecord}
     * with the supplied key values.
     */
    public static Key<ABActionRecord> getKey (int testId, String visitorId, String action)
    {
        return newKey(_R, testId, visitorId, action);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TEST_ID, VISITOR_ID, ACTION); }
    // AUTO-GENERATED: METHODS END
}
