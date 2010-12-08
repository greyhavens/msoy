//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

/**
 * Maintains a summary of how many times an action has been taken by a given member.
 */
@Entity
public class MemberActionSummaryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MemberActionSummaryRecord> _R = MemberActionSummaryRecord.class;
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Integer> ACTION_ID = colexp(_R, "actionId");
    public static final ColumnExp<Integer> COUNT = colexp(_R, "count");
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
        return newKey(_R, memberId, actionId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID, ACTION_ID); }
    // AUTO-GENERATED: METHODS END
}
