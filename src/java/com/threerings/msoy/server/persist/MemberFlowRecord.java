//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * A computed persistent entity that's used to fetch (and cache) member flow information only.
 */
@Computed
@Entity
public class MemberFlowRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberFlowRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #flow} field. */
    public static final String FLOW = "flow";

    /** The qualified column identifier for the {@link #flow} field. */
    public static final ColumnExp FLOW_C =
        new ColumnExp(MemberFlowRecord.class, FLOW);
    // AUTO-GENERATED: FIELDS END
    
    /** See {@MemberRecord#memberId}. */
    @Id
    public int memberId;

    /** See {@MemberRecord#flow}. */
    public int flow;
}
