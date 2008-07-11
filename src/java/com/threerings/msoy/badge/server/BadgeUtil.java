//
// $Id$

package com.threerings.msoy.badge.server;

import java.sql.Timestamp;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.server.persist.BadgeRecord;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyServer;

public class BadgeUtil
{
    /**
     * Awards a Badge to the specified user:
     * a. calls into BadgeRepository to create and store the BadgeRecord
     * b. record to the member's feed that they earned the stamp in question
     * c. calls MemberNodeActions.badgeAwarded
     */
    public static void awardBadge (MemberObject user, BadgeType type)
        throws PersistenceException
    {
        BadgeRecord brec = new BadgeRecord();
        brec.memberId = user.getMemberId();
        brec.badgeCode = type.getCode();
        brec.whenEarned = new Timestamp(System.currentTimeMillis());
        MsoyServer.badgeRepo.storeBadge(brec);

        MsoyServer.feedRepo.publishMemberMessage(brec.memberId, FeedMessageType.FRIEND_WON_BADGE,
            "some data here");

        MemberNodeActions.badgeAwarded(brec);
    }

}
