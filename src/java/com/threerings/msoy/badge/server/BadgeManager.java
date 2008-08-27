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

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.notify.data.BadgeEarnedNotification;
import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.notify.server.NotificationManager;

import com.threerings.msoy.badge.data.BadgeProgress;
import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;

import static com.threerings.msoy.Log.log;

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
            long now = System.currentTimeMillis();
            String levelUnits = badgeType.getRequiredUnitsString(level);
            int coinValue = badgeType.getCoinValue(level);
            badgeList.add(new EarnedBadge(badgeType.getCode(), level, levelUnits, coinValue, now));
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
        long whenEarned = System.currentTimeMillis();
        List<EarnedBadge> newBadges = Lists.newArrayList();
        List<InProgressBadge> inProgressBadges = Lists.newArrayList();
        List<InProgressBadge> deadBadges = Lists.newArrayList();
        for (BadgeType badgeType : BadgeType.values()) {
            BadgeProgress progress = badgeType.getProgress(user.stats);
            if (progress.highestLevel >= 0) {
                EarnedBadge earnedBadge = user.badges.getBadge(badgeType);
                int currentLevel = earnedBadge == null ? -1 : earnedBadge.level;
                for (int level = currentLevel + 1; level <= progress.highestLevel; level++) {
                    // award an EarnedBadge for each level that was earned in this update.
                    newBadges.add(new EarnedBadge(badgeType.getCode(), level,
                        badgeType.getRequiredUnitsString(level), badgeType.getCoinValue(level), whenEarned));
                }

                if (progress.highestLevel >= badgeType.getNumLevels()-1) {
                    // If we've reached the highest badge level, delete the existing InProgressBadge
                    // for this badge type
                    InProgressBadge inProgressBadge = user.inProgressBadges.getBadge(badgeType);
                    if (inProgressBadge != null) {
                        deadBadges.add(inProgressBadge);
                    }
                }
            }

            if (progress.highestLevel >= 0 && progress.highestLevel < badgeType.getNumLevels()-1) {
                // If we haven't reached the highest badge level for this badge, we should have a
                // corresponding InProgressBadge for it.
                InProgressBadge inProgressBadge = user.inProgressBadges.getBadge(badgeType);

                float quantizedProgress = InProgressBadgeRecord.quantizeProgress(
                    progress.getNextLevelProgress());

                if (inProgressBadge == null || progress.highestLevel >= inProgressBadge.level ||
                        (progress.highestLevel == inProgressBadge.level - 1 &&
                                quantizedProgress > inProgressBadge.progress)) {
                    int nextLevel = progress.highestLevel + 1;
                    inProgressBadges.add(new InProgressBadge(badgeType.getCode(),
                        nextLevel, badgeType.getRequiredUnitsString(nextLevel),
                        badgeType.getCoinValue(nextLevel), quantizedProgress));
                }
            }
        }

        if (!newBadges.isEmpty()) {
            awardBadges(user, newBadges);
        }

        if (!inProgressBadges.isEmpty()) {
            updateInProgressBadges(user, inProgressBadges);
        }

        if (!deadBadges.isEmpty()) {
            deleteDeadInProgressBadges(user, deadBadges);
        }
    }

    protected void awardBadges (final MemberObject user, final List<EarnedBadge> badges)
    {
        // award coins and add the badges to the user's badge set
        int coinValue = 0;
        List<Notification> notes = Lists.newArrayList();
        for (EarnedBadge badge : badges) {
            BadgeType type = BadgeType.getType(badge.badgeCode);
            BadgeType.Level level = type.getLevel(badge.level);
            if (level == null) {
                log.warning("Failed to award invalid badge level", "to", user.getMemberId(),
                            "type", type, "level", level);
            } else if (user.badgeAwarded(badge)) {
                notes.add(new BadgeEarnedNotification(badge));
                coinValue += coinValue;
            }
        }
        user.setFlow(user.flow + coinValue);
        user.setAccFlow(user.accFlow + coinValue);

        // dispatch any badge awarded notifications
        _notifyMan.notify(user, notes);

        // create any in-progress badges that have been newly unlocked
        final List<InProgressBadge> newInProgressBadges = BadgeUtil.getNewInProgressBadges(
            user.badges.asSet(), user.inProgressBadges.asSet());
        for (InProgressBadge inProgressBadge : newInProgressBadges) {
            user.inProgressBadgeUpdated(inProgressBadge);
        }

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
                for (InProgressBadge badge : newInProgressBadges) {
                    _badgeLogic.updateInProgressBadge(user.getMemberId(), badge, false);
                }
            }
            public void handleFailure (Exception error) {
                // rollback the changes to the user's BadgeSet and flow
                for (EarnedBadge badge : badges) {
                    user.badges.removeBadge(badge.badgeCode);
                }
                for (InProgressBadge badge : newInProgressBadges) {
                    user.inProgressBadges.removeBadge(badge.badgeCode);
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
                    // BadgeLogic.updateInProgressBadge handles putting the badge in the repository
                    // and publishing a member feed about the event. We don't need
                    // updateInProgressBadge to send a MemberNodeAction about this badge being
                    // earned, because we already know about it.
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

    protected void deleteDeadInProgressBadges (final MemberObject user,
        final List<InProgressBadge> badges)
    {
        // we don't need to remove the badge's from the user's in-memory InProgressBadgeSet,
        // because they will be removed automatically by MemberObject.badgeAwarded when the
        // highest level badge of a given type has been awarded.
        _invoker.postUnit(new WriteOnlyUnit("deleteDeadInProgressBadges") {
            public void invokePersist () throws PersistenceException {
                for (InProgressBadge badge : badges) {
                    _badgeLogic.deleteInProgressBadge(user.getMemberId(), badge);
                }
            }
            protected String getFailureMessage () {
                StringBuilder builder = new StringBuilder("Failed to delete in-progress badges: ");
                for (InProgressBadge badge : badges) {
                    builder.append(BadgeType.getType(badge.badgeCode).name()).append(", ");
                }
                return builder.toString();
            }
        });
    }

    @Inject protected NotificationManager _notifyMan;
    @Inject protected BadgeLogic _badgeLogic;
    @Inject protected @MainInvoker Invoker _invoker;
}
