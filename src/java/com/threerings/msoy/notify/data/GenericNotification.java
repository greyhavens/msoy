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
 */
public class GenericNotification extends Notification
{
    /** Suitable for unserialization. */
    public GenericNotification () {}

    /** Normal constructor. */
    public GenericNotification (String msg, byte category)
    {
        this(msg, category, null);
    }

    /** Normal constructor. */
    public GenericNotification (String msg, byte category, MemberName sender)
    {
        _msg = msg;
        _cat = category;
        _sender = sender;
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

    @Override
    public MemberName getSender ()
    {
        return _sender;
    }

    protected String _msg;
    protected byte _cat;
    protected MemberName _sender;
}
