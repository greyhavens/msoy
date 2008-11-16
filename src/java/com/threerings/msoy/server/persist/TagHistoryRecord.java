//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;
import com.samskivert.depot.PersistentRecord;
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
    /** The column identifier for the {@link #targetId} field. */
    public static final String TARGET_ID = "targetId";

    /** The column identifier for the {@link #tagId} field. */
    public static final String TAG_ID = "tagId";

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The column identifier for the {@link #action} field. */
    public static final String ACTION = "action";

    /** The column identifier for the {@link #time} field. */
    public static final String TIME = "time";
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
