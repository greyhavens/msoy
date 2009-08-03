//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A persisted facebook notification for use on the client.
 */
public class FacebookNotification
    implements IsSerializable
{
    /** Identifier for addressing the notification. */
    public String id;

    /** Text of the notification. */
    public String text;

    /** The most recently recorded status (e.g. "success"). */
    public String progress;

    /** The node on which the notification is scheduled or running, if any. */
    public String node;

    /** The last time the sending was started, if applicable. */
    public Date started;

    /** The last time the sending was finished, if applicable. */
    public Date finished;

    /** The number of users of the application found so far. */
    public int userCount;

    /** The number of users the notification was sent to so far. */
    public int sentCount;
}
