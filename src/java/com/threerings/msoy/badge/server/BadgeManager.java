//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.toybox.Log;

import com.threerings.msoy.badge.data.BadgeProgress;
import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;
import com.threerings.msoy.data.MemberObject;

/**
 * Handles badge related services for the world server.
 */
@Singleton @EventThread
public class BadgeManager
{
    /**
     * Awards a badge of the specified type to the user if they don't already have it.
     */
    public void awardBadge (MemberObject user, BadgeType badgeType, int level)
    {
        if (!user.badges.containsBadge(badgeType)) {
            List<EarnedBadge> badgeList = Lists.newArrayList();
            badgeList.add(new EarnedBadge(badgeType.getCode(), level, 0));
            awardBadges(user, badgeList);
        }
    }

    /**
     * For each Badge type, awards the Badge to the user if the Badge's award conditions
     * have been met.
     */
    public void updateBadges (MemberObject user)
    {
        // guests are not awarded badges
        if (user.isGuest()) {
            return;
        }

        // iterate the list of badges to see if the player has won any new ones
        List<EarnedBadge> newBadges = null;
        List<InProgressBadge> inProgressBadges = null;
        for (BadgeType badgeType : BadgeType.values()) {
            BadgeProgress progress = badgeType.getProgress(user.stats);

            if (progress.highestLevel >= 0) {
                EarnedBadge earnedBadge = user.badges.getBadge(badgeType);
                if (earnedBadge == null) {
                    // whenEarned timestamp will be filled in by awardBadges()
                    earnedBadge = new EarnedBadge(badgeType.getCode(), -1, 0);
                }
                if (earnedBadge.level < progress.highestLevel) {
                    earnedBadge.level = progress.highestLevel;

                    if (newBadges == null) {
                        newBadges = Lists.newArrayList();
                    }
                    newBadges.add(earnedBadge);
                }
            }

            if (progress.highestLevel >= 0 && progress.highestLevel < badgeType.getNumLevels()-1) {
                // If we haven't reached the highest badge level for this badge,
                // we should have a corresponding InProgressBadge for it.
                InProgressBadge inProgressBadge = user.inProgressBadges.getBadge(badgeType);
                if (inProgressBadge == null) {
                    inProgressBadge = new InProgressBadge(badgeType.getCode(), -1, 0);
                }

                float quantizedProgress = InProgressBadgeRecord.quantizeProgress(
                    progress.getNextLevelProgress());

                if (progress.highestLevel >= inProgressBadge.nextLevel ||
                        (progress.highestLevel == inProgressBadge.nextLevel - 1 &&
                                quantizedProgress > inProgressBadge.progress)) {
                    inProgressBadge.nextLevel = progress.highestLevel + 1;
                    inProgressBadge.progress = quantizedProgress;

                    if (inProgressBadges == null) {
                        inProgressBadges = Lists.newArrayList();
                    }
                    inProgressBadges.add(inProgressBadge);
                }
            }
        }

        if (newBadges != null) {
            awardBadges(user, newBadges);
        }

        if (inProgressBadges != null) {
            updateInProgressBadges(user, inProgressBadges);
        }
    }

    protected void awardBadges (final MemberObject user, final List<EarnedBadge> badges)
    {
        // stick the badges in the user's BadgeSet, and award coins
        long whenEarned = System.currentTimeMillis();
        int coinValue = 0;
        for (EarnedBadge badge : badges) {
            BadgeType type = BadgeType.getType(badge.badgeCode);
            BadgeType.Level level = type.getLevel(badge.level);
            if (level == null) {
                Log.log.warning("Failed to award invalid badge level",
                    "memberId", user.getMemberId(), "BadgeType", type, "level", level);
            } else {
                badge.whenEarned = whenEarned;
                user.badgeAwarded(badge);
                coinValue += coinValue;
            }
        }
        user.setFlow(user.flow + coinValue);
        user.setAccFlow(user.accFlow + coinValue);

        // stick the badges in the database
        final int totalCoinValue = coinValue;
        _invoker.postUnit(new WriteOnlyUnit("awardBadges") {
            public void invokePersist () throws PersistenceException {
                for (EarnedBadge badge : badges) {
                    // BadgeLogic.awardBadge handles putting the badge in the repository and
                    // publishing a member feed about the event. We don't need awardBadge()
                    // to send a MemberNodeAction about this badge being earned, because we
                    // already know about it.
                    _badgeLogic.awardBadge(user.getMemberId(), badge, false);
                }
            }
            public void handleFailure (Exception error) {
                // rollback the changes to the user's BadgeSet and flow
                for (EarnedBadge badge : badges) {
                    user.badges.removeBadge(badge.badgeCode);
                }
                user.setFlow(user.flow - totalCoinValue);
                user.setAccFlow(user.accFlow - totalCoinValue);
                super.handleFailure(error);
            }
            protected String getFailureMessage () {
                StringBuilder builder = new StringBuilder("Failed to award badges: ");
                for (EarnedBadge badge : badges) {
                    builder.append(BadgeType.getType(badge.badgeCode).name()).append(", ");
                }
                return builder.toString();
            }
        });
    }

    protected void updateInProgressBadges (final MemberObject user,
        final List<InProgressBadge> badges)
    {
        // stick the badges in the user's BadgeSet
        for (InProgressBadge badge : badges) {
            user.inProgressBadgeUpdated(badge);
        }

        // and then in the database
        _invoker.postUnit(new WriteOnlyUnit("updateInProgressBadges") {
            public void invokePersist () throws PersistenceException {
                for (InProgressBadge badge : badges) {
                    // BadgeLogic.updateInProgressBadge handles putting the badge in the repository and
                    // publishing a member feed about the event. We don't need awardBadge()
                    // to send a MemberNodeAction about this badge being earned, because we
                    // already know about it.
                    _badgeLogic.updateInProgressBadge(user.getMemberId(), badge, false);
                }
            }
            public void handleFailure (Exception error) {
                // rollback the changes to the user's BadgeSet
                for (InProgressBadge badge : badges) {
                    user.inProgressBadges.removeBadge(badge.badgeCode);
                }
                super.handleFailure(error);
            }
            protected String getFailureMessage () {
                StringBuilder builder = new StringBuilder("Failed to update in-progress badges: ");
                for (InProgressBadge badge : badges) {
                    builder.append(BadgeType.getType(badge.badgeCode).name()).append(", ");
                }
                return builder.toString();
            }
        });
    }

    @Inject protected BadgeLogic _badgeLogic;
    @Inject protected @MainInvoker Invoker _invoker;
}
