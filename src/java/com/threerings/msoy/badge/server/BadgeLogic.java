//
// $Id$

package com.threerings.msoy.badge.server;

import java.sql.Timestamp;

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
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
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
    public void awardBadge (int memberId, BadgeType type, int level, long whenEarned,
        boolean sendMemberNodeAction)
        throws PersistenceException
    {
        // ensure this is a valid badge level
        BadgeType.Level levelData = type.getLevel(level);
        if (levelData == null) {
            Log.log.warning("Failed to award invalid badge level", "memberId", memberId,
                "BadgeType", type, "level", level);
            return;
        }

        EarnedBadgeRecord brec = new EarnedBadgeRecord();
        brec.memberId = memberId;
        brec.badgeCode = type.getCode();
        brec.level = level;
        brec.whenEarned = new Timestamp(whenEarned);
        _badgeRepo.storeBadge(brec);

        _feedRepo.publishMemberMessage(memberId, FeedMessageType.FRIEND_WON_BADGE,
            "some data here");

        UserActionDetails info = new UserActionDetails(memberId, UserAction.EARNED_BADGE);
        MemberFlowRecord mfrec = _flowRepo.grantFlow(info, levelData.coinValue);

        if (sendMemberNodeAction) {
            MemberNodeActions.badgeAwarded(brec);
            MemberNodeActions.flowUpdated(mfrec);
        }
    }

    /**
     * Awards a badge to the specified member. This involves:
     * a. calling into BadgeRepository to create and store the BadgeRecord
     * b. recording to the member's feed that they earned the stamp in question
     */
    public void awardBadge (int memberId, EarnedBadge badge, boolean sendMemberNodeAction)
        throws PersistenceException
    {
        awardBadge(memberId, BadgeType.getType(badge.badgeCode), badge.level, badge.whenEarned, sendMemberNodeAction);
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
    /*public List<BadgeType> getNextBadges (int memberId, int maxBadges)
    {
        List<BadgeType> nextBadges = Lists.newArrayList();
        for (BadgeType badgeType : BadgeType.values()) {
            if (!user.badges.containsBadge(badgeType) && badgeType.isUnlocked(user)) {
                nextBadges.add(badgeType);
            }
        }

        if (!nextBadges.isEmpty()) {
            Collections.shuffle(nextBadges);
            nextBadges = nextBadges.subList(0, Math.min(maxBadges, nextBadges.size()));
        }

        return nextBadges;
    }*/

    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected FlowRepository _flowRepo;
}
