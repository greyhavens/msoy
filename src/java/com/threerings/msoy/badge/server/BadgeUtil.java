//
// $Id$

package com.threerings.msoy.badge.server;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.data.MemberObject;

public class BadgeUtil
{
    /**
     * Awards a Badge to the specified user:
     * a. calls into BadgeRepository to create and store the BadgeRecord
     * b. record to the member's feed that they earned the stamp in question
     * c. calls MemberNodeActions.badgeAwarded
     */
    public static void awardBadge (MemberObject user, BadgeType type)
    {
        // TODO
    }

}
