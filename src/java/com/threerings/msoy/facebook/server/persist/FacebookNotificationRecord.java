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
import com.threerings.msoy.admin.gwt.FacebookNotification;

/**
 * Records text to be sent to all users in the form of notifications.
 */
@Entity
public class FacebookNotificationRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 2;

    // AUTO-GENERATED: FIELDS START
    public static final Class<FacebookNotificationRecord> _R = FacebookNotificationRecord.class;
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp TEXT = colexp(_R, "text");
    public static final ColumnExp NODE = colexp(_R, "node");
    public static final ColumnExp PROGRESS = colexp(_R, "progress");
    public static final ColumnExp STARTED = colexp(_R, "started");
    public static final ColumnExp FINISHED = colexp(_R, "finished");
    public static final ColumnExp USER_COUNT = colexp(_R, "userCount");
    public static final ColumnExp SENT_COUNT = colexp(_R, "sentCount");
    // AUTO-GENERATED: FIELDS END

    /** Identifier for the notification. */
    @Id public String id;

    /** Text of the notification. */
    @Column(length=2000)
    public String text;

    /** The node on which the notification is scheduled to be sent or being sent, if any. */
    @Column(nullable=true)
    public String node;

    /** The most recently recorded status (e.g. "success"). */
    @Column(nullable=true)
    public String progress;

    /** The last time the sending was started, if applicable. */
    @Column(nullable=true)
    public Timestamp started;

    /** The last time the sending was finished, if applicable. */
    @Column(nullable=true)
    public Timestamp finished;

    /** The number of users of the application found so far. */
    public int userCount;

    /** The number of users the notification was sent to so far. */
    public int sentCount;

    /**
     * Converts this record to a runtime version.
     */
    public FacebookNotification toNotification ()
    {
        FacebookNotification notif = new FacebookNotification();
        notif.id = id;
        notif.text = text;
        notif.node = node;
        notif.progress = progress;
        notif.started = started == null ? null : new Date(started.getTime());
        notif.finished = finished == null ? null : new Date(finished.getTime());
        notif.userCount = userCount;
        notif.sentCount = sentCount;
        return notif;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FacebookNotificationRecord}
     * with the supplied key values.
     */
    public static Key<FacebookNotificationRecord> getKey (String id)
    {
        return new Key<FacebookNotificationRecord>(
                FacebookNotificationRecord.class,
                new ColumnExp[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
