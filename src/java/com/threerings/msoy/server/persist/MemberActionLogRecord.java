//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Maintains a per-member log of timestamped actions with optional extra data.
 */
@Entity
public class MemberActionLogRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String MEMBER_ID = "memberId";
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberActionLogRecord.class, MEMBER_ID);
    public static final String ACTION_ID = "actionId";
    public static final ColumnExp ACTION_ID_C =
        new ColumnExp(MemberActionLogRecord.class, ACTION_ID);
    public static final String ACTION_TIME = "actionTime";
    public static final ColumnExp ACTION_TIME_C =
        new ColumnExp(MemberActionLogRecord.class, ACTION_TIME);
    public static final String DATA = "data";
    public static final ColumnExp DATA_C = new ColumnExp(MemberActionLogRecord.class, DATA);

    /** The id of the member who performed the action. */
    public int memberId;
    
    /** The action performed. */
    public int actionId;
    
    /** When this action occured. */
    @Column(columnDefinition="actionTime DATETIME NOT NULL")
    public Timestamp actionTime;

    /** An (optional) opaque value, offline parsed/interpreted depending on the action. */
    @Column(nullable=true)
    public String data;
}
