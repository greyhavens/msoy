//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Entity;

import com.threerings.io.Streamable;

/**
 * Keeps a history of tagging events for a given target.
 */
@Entity
public abstract class TagHistoryRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TagHistoryRecord> _R = TagHistoryRecord.class;
    public static final ColumnExp TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp TAG_ID = colexp(_R, "tagId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp ACTION = colexp(_R, "action");
    public static final ColumnExp TIME = colexp(_R, "time");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** Provides the {@link #memberId} of a record. */
    public static final Function<TagHistoryRecord,Integer> GET_MEMBER_ID =
        new Function<TagHistoryRecord,Integer>() {
        public Integer apply (TagHistoryRecord record) {
            return record.memberId;
        }
    };

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
