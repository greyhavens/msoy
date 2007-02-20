//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.io.Streamable;

/**
 * Keeps a history of tagging events for a given target.
 */
@Entity
@Table
public abstract class TagHistoryRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #targetId} field. */
    public static final String TARGET_ID = "targetId";

    /** The qualified column identifier for the {@link #targetId} field. */
    public static final ColumnExp TARGET_ID_C =
        new ColumnExp(TagHistoryRecord.class, TARGET_ID);

    /** The column identifier for the {@link #tagId} field. */
    public static final String TAG_ID = "tagId";

    /** The qualified column identifier for the {@link #tagId} field. */
    public static final ColumnExp TAG_ID_C =
        new ColumnExp(TagHistoryRecord.class, TAG_ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(TagHistoryRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #action} field. */
    public static final String ACTION = "action";

    /** The qualified column identifier for the {@link #action} field. */
    public static final ColumnExp ACTION_C =
        new ColumnExp(TagHistoryRecord.class, ACTION);

    /** The column identifier for the {@link #time} field. */
    public static final String TIME = "time";

    /** The qualified column identifier for the {@link #time} field. */
    public static final ColumnExp TIME_C =
        new ColumnExp(TagHistoryRecord.class, TIME);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The ID of the target being operated on. */
    public int targetId;

    /** The ID of the tag that was added or deleted. */
    public int tagId;
    
    /** The ID of the member who added or deleted the tag. */
    public int memberId;
    
    /** The action taken (ADDED or REMOVED or COPIED). */
    public byte action;

    /** The time of the tagging event. */
    public Timestamp time;
}
