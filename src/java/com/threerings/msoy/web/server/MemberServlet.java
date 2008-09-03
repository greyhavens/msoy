//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.server.FriendManager;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.web.client.WebMemberService;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link WebMemberService}.
 */
public class MemberServlet extends MsoyServiceServlet
    implements WebMemberService
{
    // from interface WebMemberService
    public MemberCard getMemberCard (int memberId)
        throws ServiceException
    {
        for (MemberCardRecord mcr : _memberRepo.loadMemberCards(
                 Collections.singleton(memberId))) {
            return mcr.toMemberCard();
        }
        return null;
    }

    // from WebMemberService
    public boolean getFriendStatus (final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();
        return _memberRepo.getFriendStatus(memrec.memberId, memberId);
    }

    // from interface WebMemberService
    public FriendsResult loadFriends (int memberId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            return null;
        }

        FriendsResult result = new FriendsResult();
        result.name = tgtrec.getName();
        IntSet friendIds = _memberRepo.loadFriendIds(memberId);
        IntSet callerFriendIds = null;
        if (mrec != null) {
            if (mrec.memberId == memberId) {
                callerFriendIds = friendIds;
            } else {
                callerFriendIds = _memberRepo.loadFriendIds(mrec.memberId);
            }
        }
        List<MemberCard> list = _mhelper.resolveMemberCards(friendIds, false, callerFriendIds);
        Collections.sort(list, MemberHelper.SORT_BY_LAST_ONLINE);
        result.friends = list;
        return result;
    }

    // from WebMemberService
    public void addFriend (final int friendId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        _memberLogic.establishFriendship(memrec, friendId);
    }

    // from WebMemberService
    public void removeFriend (final int friendId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        _memberLogic.clearFriendship(memrec.memberId, friendId);
    }

    // from WebMemberService
    public Invitation getInvitation (String inviteId, boolean viewing)
        throws ServiceException
    {
        InvitationRecord invRec = _memberRepo.loadInvite(inviteId, viewing);
        if (invRec == null) {
            return null;
        }

        // if we're viewing this invite, log that it was viewed
        if (viewing) {
            _eventLog.inviteViewed(inviteId);
        }

        MemberName inviter = null;
        if (invRec.inviterId > 0) {
            inviter = _memberRepo.loadMemberName(invRec.inviterId);
        }
        return invRec.toInvitation(inviter);
    }

    // from WebMemberService
    public void optOut (String inviteId)
        throws ServiceException
    {
        if (_memberRepo.inviteAvailable(inviteId) != null) {
            _memberRepo.optOutInvite(inviteId);
        }
    }

    // from WebMemberService
    public List<MemberCard> getLeaderList ()
        throws ServiceException
    {
        // locate the members that match the supplied search
        IntSet mids = new ArrayIntSet();
        mids.addAll(_memberRepo.getLeadingMembers(MAX_LEADER_MATCHES));

        // resolve cards for these members
        List<MemberCard> results = _mhelper.resolveMemberCards(mids, false, null);
        Collections.sort(results, MemberHelper.SORT_BY_LEVEL);
        return results;
    }

    // from WebMemberService
    public int getABTestGroup (ReferralInfo info, String testName, boolean logEvent)
    {
        return _memberLogic.getABTestGroup(testName, info, logEvent);
    }

    // from WebMemberService
    public void trackClientAction (ReferralInfo info, String actionName, String details)
    {
        if (info == null) {
            log.warning(
                "Failed to log client action with null referral", "actionName", actionName);
            return;
        }

        _eventLog.clientAction(info.tracker, actionName, details);
    }

    // from WebMemberService
    public void trackTestAction (ReferralInfo info, String actionName, String testName)
    {
        if (info == null) {
            log.warning(
                "Failed to log test action with null referral", "actionName", actionName);
            return;
        }
        int abTestGroup = -1;
        if (testName != null) {
            // grab the group without logging a tracking event about it
            abTestGroup = _memberLogic.getABTestGroup(testName, info, false);
        } else {
            testName = "";
        }
        _eventLog.testAction(info.tracker, actionName, testName, abTestGroup);
    }

    // from WebMemberService
    public void trackReferralCreation(ReferralInfo info)
    {
        _eventLog.referralCreated(info);
    }

    // our dependencies
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected FriendManager _friendMan;
    @Inject protected MemberLogic _memberLogic;

    /** Maximum number of members to return for the leader board */
    protected static final int MAX_LEADER_MATCHES = 100;
}
