//
// $Id$

package com.threerings.msoy.notifications.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * A message for display to the user about something of interest, a suggestion or tip etc.
 */
public class Notification
    implements IsSerializable
{
    /** The type of notification. */
    public NotificationType type;

    /** The graphi to go with the message, if any. */
    public MediaDesc icon;

    /** The text of the message, if any. */
    public String text;
}
