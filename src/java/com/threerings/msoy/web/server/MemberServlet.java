//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.StringUtil;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.gwt.util.PagedResult;
import com.threerings.web.gwt.ServiceException;

// import com.threerings.msoy.admin.server.ABTestLogic;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.money.server.MoneyExchange;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.server.persist.GameInvitationRecord;
import com.threerings.msoy.person.server.persist.InvitationRecord;
import com.threerings.msoy.person.server.persist.InviteRepository;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.server.FriendManager;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.SubscriptionLogic;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.spam.server.SpamUtil;
import com.threerings.msoy.spam.server.persist.SpamRepository;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.WebCreds;
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
        Set<Integer> friendIds = _memberRepo.loadFriendIds(memberId);
        Map<Integer, Friendship> callerFriendships = null;
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
            HashSet<Integer> greeterIds = Sets.newHashSet();
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
            _memberRepo.loadMemberCards(Ints.asList(muteList), from, count, true));
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
        Map<Integer, Friendship> friendships = _memberRepo.loadFriendships(tgtrec.memberId, showingIds);
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
        Set<Integer> mids = Sets.newHashSet();
        mids.addAll(_memberRepo.getLeadingMembers(MAX_LEADER_MATCHES));

        // resolve cards for these members
        List<MemberCard> results = _mhelper.resolveMemberCards(mids, false, null);
        Collections.sort(results, MemberHelper.SORT_BY_LEVEL);
        return results;
    }

    // from WebMemberService
    public void noteNewVisitor (VisitorInfo info, String page, boolean requested)
        throws ServiceException
    {
        // String vector = StringUtil.truncate("gpage." + page, 100);

        // if (requested) {
        //     // if we requested this update from GWT, the visitor entry should already exist --
        //     // it'll just have a useless page.default vector
        //     CookieUtil.clearCookie(getThreadLocalResponse(), CookieNames.NEED_GWT_VECTOR);
        //     if (_memberRepo.updateEntryVector(info.id, vector)) {
        //         log.info("EntryVector updated", "info", info, "vector", vector);
        //         return;
        //     }
        //     log.warning("Requested entry vector update without existing vector", "info", info,
        //         "page", page);
        // }
        // // if the update was not requested by the server, GWT simply found itself without visitor
        // // information for reasons unknown and decided to make its own; insert it here
        // log.info("VisitorInfo created", "info", info, "reason", "noteNewVisitor", "page", page,
        //     "addr", getThreadLocalRequest().getRemoteAddr());
        // _memberLogic.noteNewVisitor(info, true, vector, null, 0);
    }

    // from WebMemberService
    public int getABTestGroup (VisitorInfo info, String testName, boolean logEvent)
    {
        return 0; // _testLogic.getABTestGroup(testName, info, logEvent);
    }

    // from WebMemberService
    public void logLandingABTestGroup (VisitorInfo info, String test, int group)
    {
        // int realGroup = _testLogic.getABTestGroup(test, info, true);
        // if (realGroup != group) {
        //     log.warning("Funky, the client landing group is different from the server",
        //                 "test", test, "client", group, "server", realGroup);
        // }
    }

    // from WebMemberService
    public void trackTestAction (String test, String action, VisitorInfo info)
    {
        // _testLogic.trackTestAction(test, action, info);
    }

    // from WebMemberService
    public PriceQuote getBarscriptionCost ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        if (memrec.isSubscriber()) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        return _exchange.secureQuote(Currency.BARS, _runtime.subscription.barscriptionCost, false);
    }

    // from WebMemberService
    public PurchaseResult<WebCreds.Role> barscribe (int authedBarCost)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();
        if (memrec.isSubscriber()) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        MoneyLogic.BuyOperation<WebCreds.Role> op = new MoneyLogic.BuyOperation<WebCreds.Role>() {
            public WebCreds.Role create (boolean magicFree, Currency currency, int amountPaid) {
                _subscripLogic.barscribe(memrec);
                return memrec.toRole();
            }
        };

        int cost = _runtime.subscription.barscriptionCost;
        return _moneyLogic.barscribe(memrec, authedBarCost, cost, op).toPurchaseResult();
    }

    // from interface WebMemberService
    public Boolean isThemeManager (int themeGroupId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        return (memrec.themeGroupId != 0) && (memrec.isSupport() ||
                (_groupRepo.getMembership(themeGroupId, memrec.memberId).left == Rank.MANAGER));
    }

    // from interface WebMemberService
    public GroupName[] loadManagedThemes ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        Set<Integer> groupIds = Sets.newHashSet();

        for (ThemeRecord rec : _themeRepo.getManagedThemes(memrec.memberId, Rank.MANAGER)) {
            groupIds.add(rec.groupId);
        }
        return _groupRepo.loadGroupNames(groupIds).values().toArray(new GroupName[0]);
    }

    // from interface WebMemberService
    public void escapeTheme ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        if (memrec.themeGroupId != 0) {
            _memberRepo.configureThemeId(memrec.memberId, 0);
            return;
        }
        // else they left the theme before the service call landed, that's fine with us
    }

    // our dependencies
    // @Inject protected ABTestLogic _testLogic;
    @Inject protected FriendManager _friendMan;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected InviteRepository _inviteRepo;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberManager _memberMan;
    @Inject protected MoneyExchange _exchange;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected SpamRepository _spamRepo;
    @Inject protected SubscriptionLogic _subscripLogic;
    @Inject protected ThemeRepository _themeRepo;

    /** Maximum number of members to return for the leader board */
    protected static final int MAX_LEADER_MATCHES = 100;

    /** Cutoff for adding in online greeters. */
    protected static final int NEED_FRIENDS_FRIEND_COUNT = 10;

    protected static final Object BARSCRIBE_WARE_KEY = new Object();
}
