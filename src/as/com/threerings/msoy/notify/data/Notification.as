//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.MemberName;

/**
 * Notification from the server to the client.  Notifications are sent as messages on the 
 * MemberObject.
 */
public /* abstract */ class Notification extends SimpleStreamableObject
{
    // TODO: these are just placeholder categories. These will expand and users will
    // be able to customize a filtering level.
    public static const SYSTEM :int = 0;
    public static const INVITE :int = 1;
    public static const PERSONAL :int = 2;
    public static const BUTTSCRATCHING :int = 3;

    /**
     * Get the chat message used to announce this notification, or null.
     * WTF are you doing with a null announcement?
     *
     * All announcements will be translated using the "notify" bundle.
     * You can qualify the string if you want a different bundle.
     */
    public function getAnnouncement () :String
    {
        throw new Error("Abstract");
    }
    
    /**
     * Get the category of the notification.
     */
    public function getCategory () :int
    {
        return BUTTSCRATCHING;
    }

    /**
     * Get the username of the person who sent/triggered this notification, or null
     * if this notification is not associated with another user.
     */
    public function getSender () :MemberName
    {
        return null;
    }

    /**
     * Get the special notification display to use for this notification, or
     * return null to just use the standard widget.
     */
    public function getDisplayClass () :String
    {
        return null;
    }

    // from SimpleStreamableObject
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
    }

    // from SimpleStreamableObject
    override public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error();
    }
}
}
