//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.FlowRepository;
import com.threerings.msoy.server.persist.MemberFlowRecord;

import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;

import static com.threerings.msoy.badge.Log.log;

/**
 * Provides badge related services to servlets and other blocking thread entities.
 */
@Singleton @BlockingThread
public class BadgeLogic
{
    /**
     * Awards a badge to the specified member. This involves:
     * a. calling into BadgeRepository to create and store the BadgeRecord
     * b. recording to the member's feed that they earned the stamp in question
     */
    public void awardBadge (EarnedBadgeRecord brec, boolean dobjNeedsUpdate)
        throws PersistenceException
    {
        // TODO: remove this when passport goes live
        if (!DeploymentConfig.devDeployment) {
            return;
        }

        // ensure this is a valid badge level
        BadgeType type = BadgeType.getType(brec.badgeCode);
        BadgeType.Level levelData = type.getLevel(brec.level);
        if (levelData == null) {
            log.warning("Failed to award invalid badge level", "record", brec, "type", type);
            return;
        }

        _badgeRepo.storeBadge(brec);

        _feedRepo.publishMemberMessage(brec.memberId, FeedMessageType.FRIEND_WON_BADGE,
            "some data here");

        UserActionDetails info = new UserActionDetails(brec.memberId, UserAction.EARNED_BADGE);
        MemberFlowRecord mfrec = _flowRepo.grantFlow(info, levelData.coinValue);

        if (dobjNeedsUpdate) {
            // if dobjNeedsUpdate is true, this function was called from a servlet, or other
            // blocking code, and we need to update the MemberObject if it exists. Otherwise,
            // the MemberObject initiated the badge award and does not to be updated (and also
            // will have taken care of creating new in-progress badges).
            MemberNodeActions.badgeAwarded(brec);
            MemberNodeActions.flowUpdated(mfrec);
            createNewInProgressBadges(brec.memberId, true);
        }
    }

    /**
     * Awards a badge to the specified member. This involves:
     * a. calling into BadgeRepository to create and store the BadgeRecord
     * b. recording to the member's feed that they earned the stamp in question
     */
    public void awardBadge (int memberId, EarnedBadge badge, boolean dobjNeedsUpdate)
        throws PersistenceException
    {
        awardBadge(new EarnedBadgeRecord(memberId, badge), dobjNeedsUpdate);
    }

    /**
     * Creates and stores any InProgressBadges that have been newly unlocked (as a result of a
     * member joining Whirled, or completing another badge).
     *
     * <b>NB:</b> this function makes a number of demands on the database and should be called
     * only when necessary.
     */
    public List<InProgressBadge> createNewInProgressBadges (int memberId, boolean dobjNeedsUpdate)
        throws PersistenceException
    {
        // TODO: remove this when passport goes live
        if (!DeploymentConfig.devDeployment) {
            return Collections.emptyList();
        }

        // read this member's in-progress and earned badge records
        Set<EarnedBadge> earnedBadges = Sets.newHashSet(
            Iterables.transform(_badgeRepo.loadEarnedBadges(memberId), EarnedBadgeRecord.TO_BADGE));
        Set<InProgressBadge> inProgressBadges = Sets.newHashSet(
            Iterables.transform(_badgeRepo.loadInProgressBadges(memberId),
                                InProgressBadgeRecord.TO_BADGE));

        // discover any new in-progress badges
        List<InProgressBadge> newInProgressBadges =
            _badgeMan.getNewInProgressBadges(earnedBadges, inProgressBadges);
        for (InProgressBadge badge : newInProgressBadges) {
            log.info("Created new InProgressBadge", "memberId", memberId,
                     "type", BadgeType.getType(badge.badgeCode));
            updateInProgressBadge(memberId, badge, dobjNeedsUpdate);
        }

        return newInProgressBadges;
    }

    /**
     * Creates or updates an InProgressBadge for the specified member.
     */
    public void updateInProgressBadge (InProgressBadgeRecord brec, boolean dobjNeedsUpdate)
        throws PersistenceException
    {
        _badgeRepo.storeInProgressBadge(brec);

        if (dobjNeedsUpdate) {
            MemberNodeActions.inProgressBadgeUpdated(brec);
        }
    }

    /**
     * Creates or updates an InProgressBadge for the specified member.
     */
    public void updateInProgressBadge (int memberId, InProgressBadge badge, boolean dobjNeedsUpdate)
        throws PersistenceException
    {
        updateInProgressBadge(new InProgressBadgeRecord(memberId, badge), dobjNeedsUpdate);
    }

    /**
     * Returns a List containing the set of badges that the member is working towards. This
     * method also checks that the member has had their initial set of InProgressBadgeRecords
     * created, and creates them if they haven't.
     */
    public List<InProgressBadge> getInProgressBadges (int memberId, boolean dobjNeedsUpdate)
        throws PersistenceException
    {
        if (!DeploymentConfig.devDeployment) {
            return Collections.emptyList();
        }

        // Load up our badge records
        List<InProgressBadgeRecord> badgeRecords = _badgeRepo.loadInProgressBadges(memberId);

        // If we have no badge records, our initial batch of in-progress badges hasn't yet
        // been created, and we do that now. Otherwise, transform the records into badges.
        Iterable<InProgressBadge> badges = (badgeRecords.isEmpty() ?
            createNewInProgressBadges(memberId, dobjNeedsUpdate) :
            Iterables.transform(badgeRecords, InProgressBadgeRecord.TO_BADGE));

        // Return the badges we just loaded or created, minus the "HIDDEN" marker badge.
        return Lists.newArrayList(Iterables.filter(badges, BadgeType.IS_VISIBLE_BADGE));
    }

    /**
     * Returns a set of badges for the given to pursue next. (Currently, this is just
     * a random set of in-progress badges.)
     *
     * TODO: We may want to tweak this to choose badges that reflect the member's play style or
     * previously-earned badges.
     *
     * @param maxBadges the maximum number of BadgeTypes to return.
     *
     * @return a List of BadgeTypes. The list will have maxBadges entries, unless there aren't
     * enough badges left for the player to pursue.
     */
    public List<InProgressBadge> getNextSuggestedBadges (int memberId, int maxBadges)
        throws PersistenceException
    {
        // Read in our in-progress badges and choose a number of them randomly
        List<InProgressBadge> badges = getInProgressBadges(memberId, false);
        if (!badges.isEmpty()) {
            Collections.shuffle(badges);
            badges = badges.subList(0, Math.min(maxBadges, badges.size()));
        }

        return badges;
    }

    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected FlowRepository _flowRepo;
    @Inject protected BadgeManager _badgeMan;
}
