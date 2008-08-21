//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.msoy.badge.data.all.EarnedBadge;

public class BadgeEarnedNotification extends Notification
{
    public BadgeEarnedNotification (EarnedBadge badge)
    {
        _badge = badge;
    }

    @Override
    public String getAnnouncement() {
        // no announcement for this one, it's custom
        return null;
    }

    protected EarnedBadge _badge;
}
