//
// $Id$

package com.threerings.msoy.server;

import static com.threerings.msoy.Log.log;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.ResultListener;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.ReferralRecord;

import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.web.data.MemberCard;

import com.threerings.msoy.badge.data.EarnedBadgeSet;
import com.threerings.msoy.badge.data.InProgressBadgeSet;
import com.threerings.msoy.badge.server.BadgeManager;
import com.threerings.msoy.badge.server.ServerStatSet;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.server.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

/**
 * Used to configure msoy-specific client object data.
 */
public class MsoyClientResolver extends CrowdClientResolver
{
    @Override
    public ClientObject createClientObject ()
    {
        // see if we have a member object forwarded from our peer
        final MemberObject memobj = _peerMan.getForwardedMemberObject(_username);
        return (memobj != null) ? memobj : new MemberObject();
    }

    @Override // from ClientResolver
    protected void resolveClientData (final ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        final MemberObject memobj = (MemberObject) clobj;
        memobj.setAccessController(MsoyObjectAccess.USER);

        // create a deferred notifications array so that we can track any notifications dispatched
        // to this client until they're ready to read them; we'd have NotificationManager do this
        // in a MemberLocator.Observer but we need to be sure this is filled in before any other
        // MemberLocator.Observers are notified because that's precisely when early notifications
        // are likely to be generated
        memobj.deferredNotifications = Lists.newArrayList();

        // if our member object was forwarded from another server, it will already be fully ready
        // to go so we can avoid the expensive resolution process
        if (memobj.memberName != null) {
            return;
        }

        // guests have MemberName as an auth username, members have Name
        if (_username instanceof MemberName) {
            // our auth username has our assigned name and member id, so use those
            final MemberName aname = (MemberName)_username;
            memobj.memberName = new VizMemberName(
                aname.toString(), aname.getMemberId(), MemberCard.DEFAULT_PHOTO);
            memobj.stats = new StatSet();
            memobj.badges = new EarnedBadgeSet();
            memobj.inProgressBadges = new InProgressBadgeSet();

        } else if (_username instanceof LurkerName) {
            // we are lurker, we have no visible name to speak of
            memobj.memberName = new VizMemberName("", 0, MemberCard.DEFAULT_PHOTO);
            memobj.stats = new StatSet();
            memobj.badges = new EarnedBadgeSet();
            memobj.inProgressBadges = new InProgressBadgeSet();

        } else {
            resolveMember(memobj);
        }
    }

    /**
     * Resolve a msoy member. This is called on the invoker thread.
     */
    protected void resolveMember (final MemberObject memobj)
        throws Exception
    {
        // load up their member information using on their authentication (account) name
        final MemberRecord member = _memberRepo.loadMember(_username.toString());
        final MemberMoney money = _moneyLogic.getMoneyFor(member.memberId);

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening

        // we need their profile photo to create the member name
        final ProfileRecord precord = _profileRepo.loadProfile(member.memberId);
        memobj.memberName = new VizMemberName(
            member.name, member.memberId,
            (precord == null) ? MemberCard.DEFAULT_PHOTO : precord.getPhoto());
        if (precord != null) {
            memobj.headline = precord.headline;
        }

        // configure various bits directly from their member record
        memobj.homeSceneId = member.homeSceneId;
        memobj.flow = money.getCoins();
        memobj.accFlow = (int)money.getAccCoins();
        memobj.level = member.level;

        // load up this member's persistent stats
        final List<Stat> stats = _statRepo.loadStats(member.memberId);
        memobj.stats = new ServerStatSet(stats.iterator(), _badgeMan, memobj);

        // and their badges
        memobj.badges = new EarnedBadgeSet(
            Iterables.transform(_badgeRepo.loadEarnedBadges(member.memberId),
                                EarnedBadgeRecord.TO_BADGE));
        memobj.inProgressBadges = new InProgressBadgeSet(
            Iterables.transform(_badgeRepo.loadInProgressBadges(member.memberId),
                                InProgressBadgeRecord.TO_BADGE));

//        // load up any item lists they may have
//        List<ItemListInfo> itemLists = _itemMan.getItemLists(member.memberId);
//        memobj.lists = new DSet<ItemListInfo>(itemLists);

// TEMP: flow evaporation is disabled; we need to think more about this
//         // calculate flow evaporation since last logon
//         int dT = (int) ((System.currentTimeMillis() - member.lastSession.getTime()) / 60000);
//         _memberRepo.getFlowRepository().expireFlow(member, dT); // modifies member.flow
// END TEMP

//        memobj.ownedScenes = new DSet<SceneBookmarkEntry>(
//            _sceneRepo.getOwnedScenes(member.memberId).iterator());

        // fill in this member's raw friends list; the friend manager will update it later
        memobj.friends = new DSet<FriendEntry>(_memberRepo.loadFriends(member.memberId, -1));

        // load up this member's group memberships
        memobj.groups = new DSet<GroupMembership>(
            // we don't pass in member name here because we don't need it on the client
            _groupRepo.resolveGroupMemberships(member.memberId, null).iterator());

        // load up this member's current new mail count
        memobj.newMailCount = _mailRepo.loadUnreadConvoCount(member.memberId);

        // load up their selected avatar, we'll configure it later
        if (member.avatarId != 0) {
            final AvatarRecord avatar = _itemMan.getAvatarRepository().loadItem(member.avatarId);
            if (avatar != null) {
                memobj.avatar = (Avatar)avatar.toItem();
            }
        }

        // clobber any referral information with what's in the database
        ReferralRecord refrec = _memberRepo.loadReferral(member.memberId);
        if (refrec == null) {
            // if they don't have referral info, it means they're an old user who needs to be
            // grandfathered into the new referral-tracking order of things. give them
            // a new entry with an empty affiliate, and a random tracking number.
            refrec = _memberRepo.setReferral(member.memberId,
                ReferralInfo.makeInstance("", "", "", ReferralInfo.makeRandomTracker()));
        }
        memobj.referral = refrec.toInfo();
    }

    @Override // from ClientResolver
    protected void finishResolution (final ClientObject clobj)
    {
        super.finishResolution(clobj);
        final MemberObject user = (MemberObject)clobj;

        if (!user.isGuest() && user.avatarCache == null) {
            // load up their recently used avatars
            _itemMan.loadRecentlyTouched(
                user.getMemberId(), Item.AVATAR, MemberObject.AVATAR_CACHE_SIZE,
                new ResultListener<List<Item>>() {
                public void requestCompleted (final List<Item> items) {
                    final Avatar[] avatars = new Avatar[items.size()];
                    for (int ii = 0; ii < avatars.length; ii++) {
                        avatars[ii] = (Avatar) items.get(ii);
                    }
                    user.setAvatarCache(new DSet<Avatar>(avatars));
                }
                public void requestFailed (final Exception cause) {
                    log.warning("Failed to load member's avatar cache [who=" + user.who() +
                                ", error=" + cause + "].");
                    cause.printStackTrace();
                }
            });
        }
    }

    // dependencies
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MailRepository _mailRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected StatRepository _statRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected BadgeManager _badgeMan;
}
