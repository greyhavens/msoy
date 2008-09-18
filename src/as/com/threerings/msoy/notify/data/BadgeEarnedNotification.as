//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.badge.data.all.EarnedBadge;

import com.threerings.msoy.badge.ui.BadgeNotificationDisplay;

/**
 * Notifies a user that they earned a new badge.
 */
public class BadgeEarnedNotification extends Notification
{
    public function getBadge () :EarnedBadge
    {
        return _badge;
    }

    // from Notification
    override public function getAnnouncement () :String
    {
        // TODO: wire a system that lets the player click on the notification to see the fancy
        // display again?  More notes in NotificationDisplay.displayNotification()
        return null;
    }

    // from Notification
    override public function getCategory () :int
    {
        return PERSONAL;
    }

    // from Notification 
    override public function getDisplayClass () :String
    {
        // ensure the compiler includes this class
        var c :Class = BadgeNotificationDisplay;
        return "com.threerings.msoy.badge.ui.BadgeNotificationDisplay";
    }

    // from Object
    override public function toString () :String
    {
        return "BadgeEarnedNotification [" + _badge + "]";
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _badge = ins.readObject() as EarnedBadge;
    }

    protected var _badge :EarnedBadge;
}
}
