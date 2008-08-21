//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.msoy.badge.data.all.EarnedBadge;

public class BadgeEarnedNotification extends Notification
{
    public BadgeEarnedNotification (EarnedBadge badge)
    {
        // we need a clone of this badge so that modifications don't affect this potentially
        // deferred notification.
        _badge = new EarnedBadge(badge.badgeCode, badge.level, badge.levelUnits, badge.coinValue,
            badge.whenEarned);
    }

    @Override
    public String getAnnouncement() {
        // no announcement for this one, it's custom
        return null;
    }

    @Override
    public String toString () {
        return "BadgeEarnedNotification [" + _badge + "]";
    }

    protected EarnedBadge _badge;
}
