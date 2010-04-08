//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.apps.gwt.FacebookNotificationStatus;

/**
 * Records the current state of sending a facebook notification.
 */
@Entity
public class FacebookNotificationStatusRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 2;

    // AUTO-GENERATED: FIELDS START
    public static final Class<FacebookNotificationStatusRecord> _R = FacebookNotificationStatusRecord.class;
    public static final ColumnExp APP_ID = colexp(_R, "appId");
    public static final ColumnExp BATCH_ID = colexp(_R, "batchId");
    public static final ColumnExp START_TIME = colexp(_R, "startTime");
    public static final ColumnExp PROGRESS = colexp(_R, "progress");
    public static final ColumnExp USER_COUNT = colexp(_R, "userCount");
    public static final ColumnExp SENT_COUNT = colexp(_R, "sentCount");
    // AUTO-GENERATED: FIELDS END

    /** Application this notification is being sent for. */
    @Id public int appId;

    /** Identifier for the notification batch. */
    @Id public String batchId;

    /** Time at which the batch was scheduled to start running. */
    public Timestamp startTime;

    /** The most recently recorded status (e.g. "success"). */
    @Column(nullable=true)
    public String progress;

    /** The number of users of the application found so far. */
    public int userCount;

    /** The number of users the notification was sent to so far. */
    public int sentCount;

    /**
     * Converts this record to a runtime version.
     */
    public FacebookNotificationStatus toStatus ()
    {
        FacebookNotificationStatus status = new FacebookNotificationStatus();
        status.batchId = batchId;
        status.startTime = new Date(startTime.getTime());
        status.progress = progress;
        status.userCount = userCount;
        status.sentCount = sentCount;
        return status;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FacebookNotificationStatusRecord}
     * with the supplied key values.
     */
    public static Key<FacebookNotificationStatusRecord> getKey (int appId, String batchId)
    {
        return newKey(_R, appId, batchId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(APP_ID, BATCH_ID); }
    // AUTO-GENERATED: METHODS END
}
