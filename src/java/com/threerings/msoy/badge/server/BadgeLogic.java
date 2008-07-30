//
// $Id$

package com.threerings.msoy.badge.server;

import java.sql.Timestamp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.FlowRepository;
import com.threerings.msoy.server.persist.MemberFlowRecord;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.server.persist.BadgeRecord;
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
    public void awardBadge (int memberId, BadgeType type, long whenEarned,
        boolean sendMemberNodeAction)
        throws PersistenceException
    {
        BadgeRecord brec = new BadgeRecord();
        brec.memberId = memberId;
        brec.badgeCode = type.getCode();
        brec.whenEarned = new Timestamp(whenEarned);
        _badgeRepo.storeBadge(brec);

        _feedRepo.publishMemberMessage(memberId, FeedMessageType.FRIEND_WON_BADGE,
            "some data here");

        UserActionDetails info = new UserActionDetails(memberId, UserAction.EARNED_BADGE);
        MemberFlowRecord mfrec = _flowRepo.grantFlow(info, type.getCoinValue());

        if (sendMemberNodeAction) {
            MemberNodeActions.badgeAwarded(brec);
            MemberNodeActions.flowUpdated(mfrec);
        }
    }

    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected FlowRepository _flowRepo;
}
