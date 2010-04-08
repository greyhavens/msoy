//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.apps.gwt.FacebookNotification;

/**
 * Records text to be sent to all users in the form of notifications.
 */
@Entity
public class FacebookNotificationRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 3;

    // AUTO-GENERATED: FIELDS START
    public static final Class<FacebookNotificationRecord> _R = FacebookNotificationRecord.class;
    public static final ColumnExp APP_ID = colexp(_R, "appId");
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp TEXT = colexp(_R, "text");
    // AUTO-GENERATED: FIELDS END

    /** Identifier for the notification. */
    @Id public int appId;

    /** Identifier for the notification. */
    @Id public String id;

    /** Text of the notification. */
    @Column(length=2000)
    public String text;

    /**
     * Converts this record to a runtime version.
     */
    public FacebookNotification toNotification ()
    {
        FacebookNotification notif = new FacebookNotification();
        notif.id = id;
        notif.text = text;
        return notif;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FacebookNotificationRecord}
     * with the supplied key values.
     */
    public static Key<FacebookNotificationRecord> getKey (int appId, String id)
    {
        return newKey(_R, appId, id);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(APP_ID, ID); }
    // AUTO-GENERATED: METHODS END
}
