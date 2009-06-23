//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.FriendManager;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.gwt.FacebookTemplateCard;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebMemberService;

import com.threerings.msoy.admin.server.ABTestLogic;

import com.threerings.msoy.person.server.persist.GameInvitationRecord;
import com.threerings.msoy.person.server.persist.InvitationRecord;
import com.threerings.msoy.person.server.persist.InviteRepository;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.spam.server.SpamUtil;
import com.threerings.msoy.spam.server.persist.SpamRepository;


import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.facebook.server.persist.FacebookTemplateRecord;

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
        return _memberRepo.loadMemberCard(memberId, true);
    }

    // from WebMemberService
    public Friendship getFriendship (int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();
        return _memberRepo.getTwoWayFriendship(memrec.memberId, memberId);
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
        IntMap<Friendship> callerFriendships = null;
        if ((mrec != null) && (mrec.memberId != memberId)) {
            // if we're loading someone else's friend list, we want to know whether the people
            // are OUR friends
            callerFriendships = _memberRepo.loadFriendships(mrec.memberId, friendIds);
        }
        List<MemberCard> list = _mhelper.resolveMemberCards(friendIds, false, callerFriendships);
        if ((mrec != null) && (mrec.memberId == memberId)) {
            // we are loading our own friends, let's manually fill in the FRIENDS status
            for (MemberCard card : list) {
                card.friendship = Friendship.FRIENDS;
            }
        }

        Collections.sort(list, MemberHelper.SORT_BY_LAST_ONLINE);
        result.friendsAndGreeters = list;

        // add some online greeters if this user doesn't alreay have a lot of friends and is a
        // member
        int deficit = NEED_FRIENDS_FRIEND_COUNT - list.size();
        if (padWithGreeters && mrec != null && deficit > 0) {
            HashSet<Integer> greeterIds = new HashSet<Integer>();
            greeterIds.addAll(_memberMan.getPPSnapshot().getOnlineGreeters());
            greeterIds.removeAll(friendIds);
            greeterIds.remove(mrec.memberId);
            Collection<Integer> subset = CollectionUtil.selectRandomSubset(
                greeterIds, Math.min(greeterIds.size(), deficit));
            result.friendsAndGreeters.addAll(_mhelper.resolveMemberCards(subset, true, null));
        }

        return result;
    }

    // from interface WebMemberService
    public PagedResult<MemberCard> loadMutelist (int memberId, int from, int count)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        if ((memrec.memberId != memberId) && !memrec.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        PagedResult<MemberCard> result = new PagedResult<MemberCard>();

        // TODO: possible re-implementation
        // (here, we load all memberIds, then display a page of MemberCards based on last online)
        // However, this will choke once the mutelist is greater than Short.MAX_VALUE.
        int[] muteList = _memberRepo.loadMutelist(memberId);
        result.total = muteList.length;
        result.page = MemberCardRecord.toMemberCards(
            _memberRepo.loadMemberCards(IntListUtil.asList(muteList), from, count, true));
        return result;
    }

    // from interface WebMemberService
    public void setMuted (int memberId, int muteeId, boolean muted)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        if ((memrec.memberId != memberId) && !memrec.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        _memberLogic.setMuted(memberId, muteeId, muted);
        // TODO: runtime notification? We should, but it's a PITA.
    }

    // from interface WebMemberService
    public FriendsResult loadGreeters (int offset, int limit)
        throws ServiceException
    {
        // grab the snapshot
        List<Integer> allGreeterIds = _memberMan.getPPSnapshot().getGreeters();

        // set up the name and total count
        MemberRecord tgtrec = getAuthedUser();
        FriendsResult result = new FriendsResult();
        result.name = tgtrec.getName();
        result.totalCount = allGreeterIds.size();

        // return an empty list if the offset is out of range
        if (offset >= allGreeterIds.size()) {
            result.friendsAndGreeters = Lists.newArrayList();
            return result;
        }

        // resolve the cards of the requested slice
        List<Integer> showingIds = allGreeterIds.subList(
            offset, Math.min(offset + limit, allGreeterIds.size()));
        IntMap<Friendship> friendships = _memberRepo.loadFriendships(tgtrec.memberId, showingIds);
        result.friendsAndGreeters = _mhelper.resolveMemberCards(showingIds, false, friendships);
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
        InvitationRecord invRec = _inviteRepo.loadInvite(inviteId, viewing);
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
    public Invitation getGameInvitation (String inviteId)
        throws ServiceException
    {
        GameInvitationRecord invRec = _inviteRepo.loadGameInvite(inviteId);
        if (invRec == null) {
            throw new ServiceException("e.invite_not_found");
        }
        return invRec.toInvitation();
    }

    // from WebMemberService
    public void optOut (boolean gameInvite, String inviteId)
        throws ServiceException
    {
        String email = null;
        if (gameInvite) {
            GameInvitationRecord invRec = _inviteRepo.loadGameInvite(inviteId);
            if (invRec != null) {
                email = invRec.inviteeEmail;
            }
        } else {
            InvitationRecord invRec = _inviteRepo.loadInvite(inviteId, false);
            if (invRec != null) {
                email = invRec.inviteeEmail;
            }
        }
        if (email != null) {
            _spamRepo.addOptOutEmail(email);
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
    public void noteNewVisitor (VisitorInfo info, String page)
        throws ServiceException
    {
        _memberLogic.noteNewVisitor(info, true, StringUtil.truncate("gpage." + page, 100), null);
    }

    // from WebMemberService
    public int getABTestGroup (VisitorInfo info, String testName, boolean logEvent)
    {
        return _testLogic.getABTestGroup(testName, info, logEvent);
    }

    // from WebMemberService
    public void logLandingABTestGroup (VisitorInfo info, String test, int group)
    {
        int realGroup = _testLogic.getABTestGroup(test, info, true);
        if (realGroup != group) {
            log.warning("Funky, the client landing group is different from the server",
                        "test", test, "client", group, "server", realGroup);
        }
    }

    // from WebMemberService
    public void trackTestAction (String test, String action, VisitorInfo info)
    {
        _testLogic.trackTestAction(test, action, info);
    }

    // from WebMemberService
    public FacebookTemplateCard getFacebookTemplate (String code)
        throws ServiceException
    {
        List<FacebookTemplateRecord> templates = _facebookRepo.loadVariants(code);
        if (templates.size() == 0) {
            log.warning("No Facebook templates found for request", "code", code);
            return null;
        }
        return RandomUtil.pickRandom(templates).toTemplateCard();
    }

    // our dependencies
    @Inject protected ABTestLogic _testLogic;
    @Inject protected FacebookRepository _facebookRepo;
    @Inject protected FriendManager _friendMan;
    @Inject protected InviteRepository _inviteRepo;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberManager _memberMan;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected SpamRepository _spamRepo;

    /** Maximum number of members to return for the leader board */
    protected static final int MAX_LEADER_MATCHES = 100;

    /** Cutoff for adding in online greeters. */
    protected static final int NEED_FRIENDS_FRIEND_COUNT = 10;
}
