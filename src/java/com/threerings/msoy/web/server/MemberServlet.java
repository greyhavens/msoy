//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.FriendManager;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.mail.server.SpamUtil;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebMemberService;

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
    public FriendsResult loadFriends (int memberId, boolean padWithGreeters)
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
        result.friendsAndGreeters = list;

        // Add some online greeters if this user doesn't alreay have a lot of friends
        if (padWithGreeters && list.size() < NEED_FRIENDS_FRIEND_COUNT) {
            IntSet greeterIds = new ArrayIntSet();
            greeterIds.addAll(_memberMan.getGreeterIdsSnapshot());
            greeterIds.removeAll(friendIds);
            greeterIds.remove(mrec.memberId);
            result.friendsAndGreeters.addAll(_mhelper.resolveMemberCards(greeterIds, true, null));
        }

        return result;
    }

    // from interface WebMemberService
    public FriendsResult loadGreeters ()
        throws ServiceException
    {
        MemberRecord tgtrec = getAuthedUser();
        FriendsResult result = new FriendsResult();
        result.name = tgtrec.getName();
        IntSet friendIds = _memberRepo.loadFriendIds(tgtrec.memberId);
        IntSet greeterIds = _memberRepo.loadGreeterIds();
        result.friendsAndGreeters = _mhelper.resolveMemberCards(greeterIds, false, friendIds);
        Collections.sort(result.friendsAndGreeters, MemberHelper.SORT_BY_LAST_ONLINE);
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
    public boolean isAutomaticFriender (int friendId)
        throws ServiceException
    {
        MemberRecord frec = _memberRepo.loadMember(friendId);
        return (frec != null) && frec.isGreeter();
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
    public String optOutAnnounce (int memberId, String hash)
        throws ServiceException
    {
        MemberRecord mrec = _memberRepo.loadMember(memberId);
        if (mrec == null) {
            throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
        }

        // generate an opt-out hash for this member and see if it matches
        String realHash = SpamUtil.generateOptOutHash(mrec.memberId, mrec.accountName);
        if (!hash.equals(realHash)) {
            throw new ServiceException(ServiceCodes.E_OPT_OUT_HASH_MISMATCH);
        }

        // looks good, do the deed
        mrec.setFlag(MemberRecord.Flag.NO_ANNOUNCE_EMAIL, true);
        _memberRepo.storeFlags(mrec);
        log.info("Opted " + mrec.accountName + " out of announcement emails.");
        return mrec.accountName;
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
    public int getABTestGroup (VisitorInfo info, String testName, boolean logEvent)
    {
        return _memberLogic.getABTestGroup(testName, info, logEvent);
    }

    // from WebMemberService
    public void trackClientAction (VisitorInfo info, String actionName, String details)
    {
        if (info == null) {
            log.warning(
                "Failed to log client action with null visitorInfo", "actionName", actionName);
            return;
        }

        _eventLog.clientAction(info.id, actionName, details);
    }

    // from WebMemberService
    public void trackTestAction (VisitorInfo info, String actionName, String testName)
    {
        if (info == null) {
            log.warning(
                "Failed to log test action with null visitorInfo", "actionName", actionName);
            return;
        }
        int abTestGroup = -1;
        if (testName != null) {
            // grab the group without logging a tracking event about it
            abTestGroup = _memberLogic.getABTestGroup(testName, info, false);
        } else {
            testName = "";
        }
        _eventLog.testAction(info.id, actionName, testName, abTestGroup);
    }

    // from WebMemberService
    public void trackVisitorInfoCreation (VisitorInfo info)
        throws ServiceException
    {
        _eventLog.visitorInfoCreated(info, true);
    }

    // from WebMemberService
    public void trackVectorAssociation (VisitorInfo info, String vector)
        throws ServiceException
    {
        _eventLog.vectorAssociated(info, vector);
    }

    // from WebMemberService
    public void trackHttpReferrerAssociation (VisitorInfo info, String referrer)
        throws ServiceException
    {
        _eventLog.referrerAssociated(info, referrer);
    }

    // from WebMemberService
    public void trackSessionStatusChange (VisitorInfo info, boolean guest, boolean newInfo)
    {
        _eventLog.webSessionStatusChanged(info, guest, newInfo);
    }

    // from WebMemberService
    public void debugLog (String stage, String token, String vector)
    {
        // used for WRLD-465 - TODO: remove after the test is over

        if (vector == null) {
            Matcher m = P.matcher(token);
            if (m.find() && m.groupCount() > 0) {
                vector = m.group(1) + " ?";
            }
        }

        log.info(String.format("WRLD-465 %s [%s %s]", stage, token, vector));
    }

    // our dependencies
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected FriendManager _friendMan;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberManager _memberMan;

    /** Maximum number of members to return for the leader board */
    protected static final int MAX_LEADER_MATCHES = 100;

    /** Cutoff for adding in online greeters. */
    protected static final int NEED_FRIENDS_FRIEND_COUNT = 10;

    private static final Pattern P = Pattern.compile("vec_([^_]+)");
}
