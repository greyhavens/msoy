//
// $Id$

package com.threerings.msoy.badge.server;

import com.threerings.msoy.badge.data.Badge;
import com.threerings.msoy.data.MemberObject;

public class BadgeManager
{
    /**
     * For each Badge type, awards the Badge to the user if the Badge's award conditions
     * have been met.
     */
    public static void updateBadges (MemberObject user)
    {
        for (Badge.Type badgeType : Badge.Type.values()) {
            // TODO if badge has been completed and has not yet been awarded, award it
        }
    }
}
