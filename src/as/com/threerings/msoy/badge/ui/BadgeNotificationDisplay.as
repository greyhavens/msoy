//
// $Id$

package com.threerings.msoy.badge.ui {

import com.threerings.util.Log;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.notify.data.BadgeEarnedNotification;
import com.threerings.msoy.notify.data.Notification;

public class BadgeNotificationDisplay
{
    public function init (wctx :WorldContext, notification :Notification) :void
    {
        var badgeNotif :BadgeEarnedNotification = notification as BadgeEarnedNotification;
        if (badgeNotif == null) {
            log.warning("received bad badge notification! [" + notification + "]");
        }
        wctx.displayAward(badgeNotif.getBadge());
    }

    protected static const log :Log = Log.getLog(BadgeNotificationDisplay);
}
}
