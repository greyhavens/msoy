//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.person.data.MailFolder;
import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.person.server.persist.MailRepository;
import com.threerings.msoy.person.server.persist.ProfileRecord;

import com.threerings.msoy.group.data.GroupMembership;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.persist.AvatarRecord;

import com.threerings.msoy.data.GuestName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.server.persist.MemberRecord;

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
        MemberObject memobj = MsoyServer.peerMan.getForwardedMemberObject(_username);
        return (memobj != null) ? memobj : new MemberObject();
    }

    @Override // from ClientResolver
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        MemberObject userObj = (MemberObject) clobj;
        userObj.setAccessController(MsoyObjectAccess.USER);
        if (_username instanceof GuestName) {
            resolveGuest(userObj);
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
        // if our member object was forwarded from another server, it will already be fully ready
        // to go so we can avoid a whole bunch of expensive database lookups
        if (userObj.memberName != null) {
            return;
        }

        // load up their member information using on their authentication (account) name
        MemberRecord member = MsoyServer.memberRepo.loadMember(_username.toString());

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening

        // we need their profile photo to create the member name
        ProfileRecord precord = MsoyServer.profileRepo.loadProfile(member.memberId);
        userObj.memberName = new VizMemberName(
            member.name, member.memberId,
            (precord == null) ? Profile.DEFAULT_PHOTO : precord.getPhoto());

        // configure various bits directly from their member record
        userObj.homeSceneId = member.homeSceneId;
        userObj.flow = member.flow;
        userObj.accFlow = member.accFlow;
        userObj.level = member.level;
        userObj.humanity = member.humanity;

        // load up this member's persistent stats
        List<Stat> stats = MsoyServer.statRepo.loadStats(member.memberId);
        userObj.stats = new StatSet(stats.iterator());

//        // load up any item lists they may have
//        List<ItemListInfo> itemLists = MsoyServer.itemMan.getItemLists(member.memberId);
//        userObj.lists = new DSet<ItemListInfo>(itemLists);

// TEMP: flow evaporation is disabled; we need to think more about this
//         // calculate flow evaporation since last logon
//         int dT = (int) ((System.currentTimeMillis() - member.lastSession.getTime()) / 60000);
//         MsoyServer.memberRepo.getFlowRepository().expireFlow(member, dT); // modifies member.flow
// END TEMP

        userObj.ownedScenes = new DSet<SceneBookmarkEntry>(
            MsoyServer.sceneRepo.getOwnedScenes(member.memberId).iterator());

        // fill in this member's raw friends list; the friend manager will update it later
        userObj.friends = new DSet<FriendEntry>(
            MsoyServer.memberRepo.loadFriends(member.memberId, -1));

        // load up this member's group memberships
        userObj.groups = new DSet<GroupMembership>(
            // we don't pass in member name here because we don't need it on the client
            MsoyServer.groupRepo.resolveGroupMemberships(member.memberId, null).iterator());

        // load up this member's current new mail message count
        MailRepository mailRepo = MsoyServer.mailMan.getRepository();
        Tuple<Integer, Integer> count = mailRepo.getMessageCount(
            member.memberId, MailFolder.INBOX_FOLDER_ID);
        userObj.newMailCount = count.right;

        // load up their selected avatar, we'll configure it later
        if (member.avatarId != 0) {
            AvatarRecord avatar =
                MsoyServer.itemMan.getAvatarRepository().loadItem(member.avatarId);
            if (avatar != null) {
                userObj.avatar = (Avatar)avatar.toItem();
            }
        }
    }

    /**
     * Resolve a lowly guest. This is called on the invoker thread.
     */
    protected void resolveGuest (MemberObject userObj)
        throws Exception
    {
        userObj.memberName = new VizMemberName(
            _username.toString(), MemberName.GUEST_ID, Profile.DEFAULT_PHOTO);
    }

    @Override // from ClientResolver
    protected void finishResolution (ClientObject clobj)
    {
        super.finishResolution(clobj);
        final MemberObject user = (MemberObject)clobj;

        if (!user.isGuest() && user.avatarCache == null) {
            // load up their recently used avatars
            MsoyServer.itemMan.loadRecentlyTouched(
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
}
