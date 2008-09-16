//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Maintains a summary of how many times an action has been taken by a given member.
 */
@Entity
public class MemberActionSummaryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberActionSummaryRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #actionId} field. */
    public static final String ACTION_ID = "actionId";

    /** The qualified column identifier for the {@link #actionId} field. */
    public static final ColumnExp ACTION_ID_C =
        new ColumnExp(MemberActionSummaryRecord.class, ACTION_ID);

    /** The column identifier for the {@link #count} field. */
    public static final String COUNT = "count";

    /** The qualified column identifier for the {@link #count} field. */
    public static final ColumnExp COUNT_C =
        new ColumnExp(MemberActionSummaryRecord.class, COUNT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The id of the member who's performed the action. */
    @Id
    public int memberId;

    @Id
    /** The action performed. */
    public int actionId;

    /** How many times this action has occured. */
    public int count;

    /** An empty constructor for Depot's benefit. */
    public MemberActionSummaryRecord ()
    {
    }

    /** Creates a fully configured {@link MemberActionSummaryRecord}. */
    public MemberActionSummaryRecord (int memberId, int actionId, int count)
    {
        this.memberId = memberId;
        this.actionId = actionId;
        this.count = count;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemberActionSummaryRecord}
     * with the supplied key values.
     */
    public static Key<MemberActionSummaryRecord> getKey (int memberId, int actionId)
    {
        return new Key<MemberActionSummaryRecord>(
                MemberActionSummaryRecord.class,
                new String[] { MEMBER_ID, ACTION_ID },
                new Comparable[] { memberId, actionId });
    }
    // AUTO-GENERATED: METHODS END
}
