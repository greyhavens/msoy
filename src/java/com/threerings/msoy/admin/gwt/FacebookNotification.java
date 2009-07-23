//
// $Id$

package com.threerings.msoy.admin.gwt;

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
}
