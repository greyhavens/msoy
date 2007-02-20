//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Maintains a summary of how many times an action has been taken by a given member.
 */
@Entity
public class MemberActionSummaryRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String MEMBER_ID = "memberId";
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberActionSummaryRecord.class, MEMBER_ID);
    public static final String ACTION_ID = "actionId";
    public static final ColumnExp ACTION_ID_C =
        new ColumnExp(MemberActionSummaryRecord.class, ACTION_ID);
    public static final String COUNT = "count";
    public static final ColumnExp COUNT_C = new ColumnExp(MemberActionSummaryRecord.class, COUNT);

    /** The id of the member who's performed the action. */
    @Id
    public int memberId;

    @Id
    /** The action performed. */
    public int actionId;
    
    /** How many times this action has occured. */
    public int count;
}
