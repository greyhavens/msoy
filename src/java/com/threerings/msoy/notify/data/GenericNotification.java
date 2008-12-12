//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.all.MemberName;

/**
 * Used for two purposes:
 *  - Generate a notification purely in the client, from the NotificationDirector
 *    noticing some change on the MemberObject or something.
 *  - Sending a notification to the client without creating a custom class. Custom classes
 *    are often preferred since they can compress the notification data
 *
 * If you need to specify a sender, it's time to write a custom class, brah.
 */
public class GenericNotification extends Notification
{
    /** Suitable for unserialization. */
    public GenericNotification () {}

    /** Normal constructor. */
    public GenericNotification (String msg, byte category)
    {
        _msg = msg;
        _cat = category;
    }

    @Override
    public String getAnnouncement ()
    {
        return _msg;
    }

    @Override
    public byte getCategory ()
    {
        return _cat;
    }

    protected String _msg;
    protected byte _cat;
}
