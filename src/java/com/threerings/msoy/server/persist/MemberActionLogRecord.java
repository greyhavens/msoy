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
@Entity
public class MemberActionLogRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MemberActionLogRecord> _R = MemberActionLogRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp ACTION_ID = colexp(_R, "actionId");
    public static final ColumnExp ACTION_TIME = colexp(_R, "actionTime");
    public static final ColumnExp DATA = colexp(_R, "data");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The id of the member who performed the action. */
    @Index(name="ixMember")
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
