//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.ResultListener;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.web.data.MemberCard;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.AvatarRecord;

import com.threerings.msoy.badge.data.BadgeSet;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.server.persist.BadgeRecord;
import com.threerings.msoy.badge.server.persist.BadgeRepository;

import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;
// import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.ReferralRecord;

import static com.threerings.msoy.Log.log;

/**
 * Used to configure msoy-specific client object data.
 */
public class MsoyClientResolver extends CrowdClientResolver
{
    @Override
    public ClientObject createClientObject ()
    {
        // see if we have a member object forwarded from our peer
        MemberObject memobj = _peerMan.getForwardedMemberObject(_username);
        return (memobj != null) ? memobj : new MemberObject();
    }

    @Override // from ClientResolver
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        MemberObject userObj = (MemberObject) clobj;
        userObj.setAccessController(MsoyObjectAccess.USER);

        // if our member object was forwarded from another server, it will already be fully ready
        // to go so we can avoid the expensive resolution process
        if (userObj.memberName != null) {
            return;
        }

        // guests have MemberName as an auth username, members have Name
        if (_username instanceof MemberName) {
            // our auth username has our assigned name and member id, so use those
            MemberName aname = (MemberName)_username;
            userObj.memberName = new VizMemberName(
                aname.toString(), aname.getMemberId(), MemberCard.DEFAULT_PHOTO);

        } else if (_username instanceof LurkerName) {
            // we are lurker, we have no visible name to speak of
            userObj.memberName = new VizMemberName("", 0, MemberCard.DEFAULT_PHOTO);

        } else {
            resolveMember(userObj);
        }
    }

    /**
     * Resolve a msoy member. This is called on the invoker thread.
     */
    protected void resolveMember (MemberObject userObj)
        throws Exception
    {
        // load up their member information using on their authentication (account) name
        MemberRecord member = _memberRepo.loadMember(_username.toString());

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening

        // we need their profile photo to create the member name
        ProfileRecord precord = _profileRepo.loadProfile(member.memberId);
        userObj.memberName = new VizMemberName(
            member.name, member.memberId,
            (precord == null) ? MemberCard.DEFAULT_PHOTO : precord.getPhoto());
        if (precord != null) {
            userObj.headline = precord.headline;
        }

        // configure various bits directly from their member record
        userObj.homeSceneId = member.homeSceneId;
        userObj.flow = member.flow;
        userObj.accFlow = member.accFlow;
        userObj.level = member.level;

        // Ensure that the StatType enum is loaded before StatRepo.loadStats() is called
        @SuppressWarnings("unused") StatType dummy = StatType.UNUSED;
        // load up this member's persistent stats
        List<Stat> stats = _statRepo.loadStats(member.memberId);
        userObj.stats = new StatSet(stats.iterator());

        // and their badges
        List<BadgeRecord> badgeRecs = _badgeRepo.loadBadges(member.memberId);
        List<EarnedBadge> badges = Lists.newArrayListWithExpectedSize(badgeRecs.size());
        for (BadgeRecord rec : badgeRecs) {
            badges.add(rec.toBadge());
        }
        userObj.badges = new BadgeSet(badges);

//        // load up any item lists they may have
//        List<ItemListInfo> itemLists = _itemMan.getItemLists(member.memberId);
//        userObj.lists = new DSet<ItemListInfo>(itemLists);

// TEMP: flow evaporation is disabled; we need to think more about this
//         // calculate flow evaporation since last logon
//         int dT = (int) ((System.currentTimeMillis() - member.lastSession.getTime()) / 60000);
//         _memberRepo.getFlowRepository().expireFlow(member, dT); // modifies member.flow
// END TEMP

//        userObj.ownedScenes = new DSet<SceneBookmarkEntry>(
//            _sceneRepo.getOwnedScenes(member.memberId).iterator());

        // fill in this member's raw friends list; the friend manager will update it later
        userObj.friends = new DSet<FriendEntry>(
            _memberRepo.loadFriends(member.memberId, -1));

        // load up this member's group memberships
        userObj.groups = new DSet<GroupMembership>(
            // we don't pass in member name here because we don't need it on the client
            _groupRepo.resolveGroupMemberships(member.memberId, null).iterator());

        // load up this member's current new mail count
        userObj.newMailCount = _mailRepo.loadUnreadConvoCount(member.memberId);

        // load up their selected avatar, we'll configure it later
        if (member.avatarId != 0) {
            AvatarRecord avatar = _itemMan.getAvatarRepository().loadItem(member.avatarId);
            if (avatar != null) {
                userObj.avatar = (Avatar)avatar.toItem();
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
        userObj.referral = refrec.toInfo();
    }

    @Override // from ClientResolver
    protected void finishResolution (ClientObject clobj)
    {
        super.finishResolution(clobj);
        final MemberObject user = (MemberObject)clobj;

        if (!user.isGuest() && user.avatarCache == null) {
            // load up their recently used avatars
            _itemMan.loadRecentlyTouched(
                user.getMemberId(), Item.AVATAR, MemberObject.AVATAR_CACHE_SIZE,
                new ResultListener<List<Item>>() {
                public void requestCompleted (List<Item> items) {
                    Avatar[] avatars = new Avatar[items.size()];
                    for (int ii = 0; ii < avatars.length; ii++) {
                        avatars[ii] = (Avatar) items.get(ii);
                    }
                    user.setAvatarCache(new DSet<Avatar>(avatars));
                }
                public void requestFailed (Exception cause) {
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
}
