//
// $Id$

package com.threerings.msoy.facebook.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * The status of a Facebook notification.
 */
public class NotificationStatus
    implements IsSerializable
{
    /**
     * Creates a new notification result for serialization.
     */
    public NotificationStatus ()
    {
    }

    /**
     * Creates a new notification result with the given id.
     */
    public NotificationStatus (String id)
    {
        this.id = id;
        this.status = "New";
    }

    /**
     * Creates a copy of this status.
     */
    public NotificationStatus clone ()
    {
        NotificationStatus copy = new NotificationStatus();
        copy.id = id;
        copy.status = status;
        copy.start = start;
        copy.finished = finished;
        copy.userCount = userCount;
        return copy;
    }

    /** The id of the notification. */
    public String id;

    /** The most recently recorded status (e.g. "success"). */
    public String status;

    /** The last time the sending was started, or null if it has not been started yet. */
    public Date start;

    /** The last time the sending was finished, or null if it is currently running or not
     * started. */
    public Date finished;

    /** The number of users the notification was sent to, or zero if not applicable. */
    public int userCount;
}
