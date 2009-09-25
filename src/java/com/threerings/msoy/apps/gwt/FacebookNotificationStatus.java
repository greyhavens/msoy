//
// $Id$

package com.threerings.msoy.apps.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents the sending of a batch of notifications.
 */
public class FacebookNotificationStatus
    implements IsSerializable
{
    /** Unique identifier (per application) for addressing the batch. */
    public String batchId;

    /** When the notification was scheduled to start. */
    public Date startTime;

    /** The most recently recorded status (e.g. "success"). */
    public String progress;

    /** The number of users of the application found so far. */
    public int userCount;

    /** The number of users the notification was sent to so far. */
    public int sentCount;
}
