//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;

/**
 * Contains badge-related utility methods.
 */
public class BadgeUtil
{
    /**
     * @return a List of InProgressBadges, for badges that have been newly unlocked (as a result
     * of a member joining Whirled, or completing another badge).
     */
    public static List<InProgressBadge> getNewInProgressBadges (
        Set<EarnedBadge> earnedBadges, Set<InProgressBadge> inProgressBadges)
    {
        // Construct a set of badges that contains all earned and in-progress badges.
        Set<Badge> existingBadges = Sets.union(earnedBadges, inProgressBadges);

        List<InProgressBadge> newBadges = Lists.newArrayList();
        for (BadgeType badgeType : BadgeType.visibleValues()) {
            // create a dummy badge to check if this type is already in the set of existing badges
            // All badges check equality and hash purely based on their badge code.
            Badge dummyBadge = new Badge(badgeType.getCode(), 0, null, 0) {
                @Override public String imageUrl () { return ""; }
            };

            // If the badge is newly unlocked, add it to our list. Note that the badge's progress
            // is set to 0, while the player may actually have made some progress on it. Progress
            // will be correctly updated next time the player bumps the stat that this badge
            // depends on.
            if (!existingBadges.contains(dummyBadge) && badgeType.isUnlocked(earnedBadges)) {
                float progress = badgeType.progressValid(0) ? 0 : -1;
                newBadges.add(new InProgressBadge(badgeType.getCode(), 0,
                    badgeType.getRequiredUnitsString(0), badgeType.getCoinValue(0), progress));
            }
        }

        return newBadges;
    }
}
