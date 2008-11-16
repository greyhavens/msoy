//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

/**
 * Maintains a per-member log of timestamped actions with optional extra data.
 */
@Entity(indices={
    @Index(name="ixMember", fields={ MemberActionLogRecord.MEMBER_ID })
})
public class MemberActionLogRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberActionLogRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #actionId} field. */
    public static final String ACTION_ID = "actionId";

    /** The qualified column identifier for the {@link #actionId} field. */
    public static final ColumnExp ACTION_ID_C =
        new ColumnExp(MemberActionLogRecord.class, ACTION_ID);

    /** The column identifier for the {@link #actionTime} field. */
    public static final String ACTION_TIME = "actionTime";

    /** The qualified column identifier for the {@link #actionTime} field. */
    public static final ColumnExp ACTION_TIME_C =
        new ColumnExp(MemberActionLogRecord.class, ACTION_TIME);

    /** The column identifier for the {@link #data} field. */
    public static final String DATA = "data";

    /** The qualified column identifier for the {@link #data} field. */
    public static final ColumnExp DATA_C =
        new ColumnExp(MemberActionLogRecord.class, DATA);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The id of the member who performed the action. */
    public int memberId;

    /** The action performed. */
    public int actionId;

    /** When this action occured. */
    public Timestamp actionTime;

    /** An (optional) opaque value, offline parsed/interpreted depending on the action. */
    @Column(nullable=true)
    public String data;

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
