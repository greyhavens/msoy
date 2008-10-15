//
// $Id$

package com.threerings.msoy.badge.ui {

import com.threerings.util.Log;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.notify.data.BadgeEarnedNotification;
import com.threerings.msoy.notify.data.Notification;

public class BadgeNotificationDisplay
{
    public function init (ctx :MsoyContext, notification :Notification) :void
    {
        var badgeNotif :BadgeEarnedNotification = notification as BadgeEarnedNotification;
        if (badgeNotif == null) {
            Log.getLog(this).warning("received bad badge notification! [" + notification + "]");
        }
        ctx.getNotificationDirector().displayAward(badgeNotif.getBadge());
    }
}
}
