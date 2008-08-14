//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.toybox.Log;

import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.FlowRepository;
import com.threerings.msoy.server.persist.MemberFlowRecord;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;

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
        // ensure this is a valid badge level
        BadgeType type = BadgeType.getType(brec.badgeCode);
        BadgeType.Level levelData = type.getLevel(brec.level);
        if (levelData == null) {
            Log.log.warning("Failed to award invalid badge level", "EarnedBadgeRecord", brec,
                "BadgeType", type);
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
            createNewInProgressBadges(brec.memberId);
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
    public void createNewInProgressBadges (int memberId)
        throws PersistenceException
    {
        // read this member's in-progress and earned badge records
        List<EarnedBadgeRecord> earnedBadgeRecs = _badgeRepo.loadEarnedBadges(memberId);
        Set<EarnedBadge> earnedBadges = Sets.newHashSetWithExpectedSize(earnedBadgeRecs.size());
        for (EarnedBadgeRecord brec : earnedBadgeRecs) {
            earnedBadges.add(brec.toBadge());
        }

        List<InProgressBadgeRecord> inProgressBadgeRecs = _badgeRepo.loadInProgressBadges(memberId);
        Set<InProgressBadge> inProgressBadges = Sets.newHashSetWithExpectedSize(
            inProgressBadgeRecs.size());
        for (InProgressBadgeRecord brec : inProgressBadgeRecs) {
            inProgressBadges.add(brec.toBadge());
        }

        // discover any new in-progress badges
        List<InProgressBadge> newInProgressBadges = _badgeMan.getNewInProgressBadges(earnedBadges,
            inProgressBadges);
        for (InProgressBadge badge : newInProgressBadges) {
            Log.log.info("created new InProgressBadge", "memberId", memberId,
                "type", BadgeType.getType(badge.badgeCode));
            updateInProgressBadge(memberId, badge, true);
        }
    }

    /**
     * Creates or updates an InProgressBadge for the specified member.
     */
    public void updateInProgressBadge (InProgressBadgeRecord brec, boolean sendMemberNodeAction)
        throws PersistenceException
    {
        _badgeRepo.storeInProgressBadge(brec);

        if (sendMemberNodeAction) {
            MemberNodeActions.inProgressBadgeUpdated(brec);
        }
    }

    /**
     * Creates or updates an InProgressBadge for the specified member.
     */
    public void updateInProgressBadge (int memberId, InProgressBadge badge,
        boolean sendMemberNodeAction)
        throws PersistenceException
    {
        updateInProgressBadge(new InProgressBadgeRecord(memberId, badge), sendMemberNodeAction);
    }

    /**
     * Returns a set of badges for the given to pursue next. (Currently, this is just
     * a random set of unlocked badges.)
     *
     * TODO: We may want to tweak this to choose badges that reflect the member's play style or
     * previously-earned badges.
     *
     * @param maxBadges the maximum number of BadgeTypes to return.
     *
     * @return a List of BadgeTypes. The list will have maxBadges entries, unless there aren't
     * enough badges left for the player to pursue.
     */
    public List<InProgressBadge> getNextBadges (int memberId, int maxBadges)
        throws PersistenceException
    {
        List<InProgressBadge> nextBadges = Lists.newArrayList();

        // Read in our in-progress badges and choose a number of them randomly
        List<InProgressBadgeRecord> badgeRecords = _badgeRepo.loadInProgressBadges(memberId);
        if (!badgeRecords.isEmpty()) {
            Collections.shuffle(badgeRecords);
            badgeRecords = badgeRecords.subList(0, Math.min(maxBadges, badgeRecords.size()));
            for (InProgressBadgeRecord brec : badgeRecords) {
                nextBadges.add(brec.toBadge());
            }
        }

        return nextBadges;
    }

    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected FlowRepository _flowRepo;
    @Inject protected BadgeManager _badgeMan;
}
