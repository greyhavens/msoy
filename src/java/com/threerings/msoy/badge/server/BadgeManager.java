//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MemberLocal;

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
     * For each Badge type, awards the Badge to the user if the Badge's award conditions
     * have been met.
     */
    public void updateBadges (MemberObject user)
    {
        // guests are not awarded badges
        if (user.isGuest()) {
            return;
        }
        MemberLocal local = user.getLocal(MemberLocal.class);

        // iterate the list of badges to see if the player has won any new ones
        long whenEarned = System.currentTimeMillis();
        List<EarnedBadge> newBadges = Lists.newArrayList();
        List<InProgressBadge> inProgressBadges = Lists.newArrayList();
        List<InProgressBadge> deadBadges = Lists.newArrayList();
        for (BadgeType badgeType : BadgeType.values()) {
            BadgeProgress progress = badgeType.getProgress(local.stats);
            if (progress.highestLevel < 0) {
                // skip badges with no progress
                // TODO: retroactive unawarding?
                continue;
            }

            EarnedBadge earnedBadge = local.badges.getBadge(badgeType);
            int currentLevel = earnedBadge == null ? -1 : earnedBadge.level;

            // award an EarnedBadge for each level that was earned in this update.
            for (int level = currentLevel + 1; level <= progress.highestLevel; level++) {
                newBadges.add(new EarnedBadge(badgeType.getCode(), level,
                                              badgeType.getRequiredUnitsString(level),
                                              badgeType.getCoinValue(level), whenEarned));
            }

            if (progress.highestLevel >= badgeType.getNumLevels()-1) {
                // If we've reached the highest badge level, delete the existing InProgressBadge
                // for this badge type
                InProgressBadge inProgressBadge = local.inProgressBadges.getBadge(badgeType);
                if (inProgressBadge != null) {
                    deadBadges.add(inProgressBadge);
                }

            } else {
                // ... otherwise make sure we have an InProgressBadge for it.
                InProgressBadge inProgressBadge = local.inProgressBadges.getBadge(badgeType);

                float quantizedProgress = InProgressBadgeRecord.quantizeProgress(
                    progress.getNextLevelProgress());

                if (inProgressBadge == null || progress.highestLevel >= inProgressBadge.level ||
                        (progress.highestLevel == inProgressBadge.level - 1 &&
                            // TODO: fp epsilon?
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
        final MemberLocal local = user.getLocal(MemberLocal.class);

        // award coins and add the badges to the user's badge set
        List<Notification> notes = Lists.newArrayList();
        for (EarnedBadge badge : badges) {
            BadgeType type = BadgeType.getType(badge.badgeCode);
            BadgeType.Level level = type.getLevel(badge.level);
            if (level == null) {
                log.warning("Failed to award invalid badge level", "to", user.getMemberId(),
                            "type", type, "level", level);
            } else if (local.badgeAwarded(badge)) {
                notes.add(new BadgeEarnedNotification(badge));
            }
        }

        // dispatch any badge awarded notifications
        _notifyMan.notify(user, notes);

        // create any in-progress badges that have been newly unlocked
        final List<InProgressBadge> newInProgressBadges = BadgeUtil.getNewInProgressBadges(
            local.badges.asSet(), local.inProgressBadges.asSet());
        for (InProgressBadge inProgressBadge : newInProgressBadges) {
            local.inProgressBadgeUpdated(inProgressBadge);
        }

        // stick the badges in the database
        _invoker.postUnit(new WriteOnlyUnit("awardBadges") {
            public void invokePersist () throws Exception {
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
                    local.badges.removeBadge(badge.badgeCode);
                }
                for (InProgressBadge badge : newInProgressBadges) {
                    local.inProgressBadges.removeBadge(badge.badgeCode);
                }
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
        final MemberLocal local = user.getLocal(MemberLocal.class);
        for (InProgressBadge badge : badges) {
            local.inProgressBadgeUpdated(badge);
        }

        // and then in the database
        _invoker.postUnit(new WriteOnlyUnit("updateInProgressBadges") {
            public void invokePersist () throws Exception {
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
                    local.inProgressBadges.removeBadge(badge.badgeCode);
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
            public void invokePersist () throws Exception {
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
