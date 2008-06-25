//
// $Id$

package com.threerings.msoy.notify.data;

import java.io.Serializable;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.ActionScript;

/**
 * Notification from the server to the client.
 */
public abstract class Notification extends SimpleStreamableObject
    implements Serializable
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
}
