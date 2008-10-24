//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.ActionScript;

import com.threerings.msoy.data.all.MemberName;

/**
 * Notification from the server to the client.
 */
public abstract class Notification extends SimpleStreamableObject
{
    @ActionScript(omit=true)
    public Notification ()
    {
        // nothing special
    }

    /**
     * Get the chat message used to announce this notification, or null.
     * WTF are you doing with a null announcement.
     *
     * All announcements will be translated using the "notify" bundle.
     * You can qualify the string if you want a different bundle.
     */
    public abstract String getAnnouncement ();

    /**
     * Get the username of the person who sent/triggered this notification, or null
     * if this notification is not associated with another user.
     */
    public MemberName getSender ()
    {
        return null;
    }
}
