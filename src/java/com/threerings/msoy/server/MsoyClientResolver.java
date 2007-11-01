//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.person.data.MailFolder;
import com.threerings.msoy.person.server.persist.MailRepository;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.persist.AvatarRecord;

import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;

import static com.threerings.msoy.Log.log;

/**
 * Used to configure msoy-specific client object data.
 */
public class MsoyClientResolver extends CrowdClientResolver
{
    @Override
    public ClientObject createClientObject ()
    {
        return new MemberObject();
    }

    @Override // from PresentsClient
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        MemberObject userObj = (MemberObject) clobj;
        userObj.setAccessController(MsoyObjectAccess.USER);
        if (isResolvingGuest()) {
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
        // load up their member information using on their authentication (account) name
        MemberRecord member = MsoyServer.memberRepo.loadMember(_username.toString());

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening

        // configure various bits directly from their member record
        userObj.memberName = member.getName();
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

        // load up this member's group memberships (TODO: do this in one lookup)
        List<GroupMembership> groups = Lists.newArrayList();
        for (GroupMembershipRecord record : MsoyServer.groupRepo.getMemberships(member.memberId)) {
            GroupRecord group = MsoyServer.groupRepo.loadGroup(record.groupId);
            if (group == null) {
                log.warning("User member of non-existent group?! [who=" + member.accountName +
                            ", groupId=" + record.groupId + "].");
                continue;
            }
            groups.add(record.toGroupMembership(group, null));
        }
        userObj.groups = new DSet<GroupMembership>(groups.iterator());

        // load up this member's current new mail message count
        MailRepository mailRepo = MsoyServer.mailMan.getRepository();
        Tuple<Integer, Integer> count = mailRepo.getMessageCount(
            member.memberId, MailFolder.INBOX_FOLDER_ID);
        userObj.hasNewMail = (count.right > 0);

        // load up their selected avatar, we'll configure it later
        if (member.avatarId != 0) {
            AvatarRecord avatar =
                MsoyServer.itemMan.getAvatarRepository().loadItem(member.avatarId);
            if (avatar != null) {
                userObj.avatar = (Avatar)avatar.toItem();
            }
        }

        userObj.avrGameId = member.avrGameId;
    }

    /**
     * Resolve a lowly guest. This is called on the invoker thread.
     */
    protected void resolveGuest (MemberObject userObj)
        throws Exception
    {
        userObj.setMemberName((MemberName) _username);
    }

    @Override // from PresentsClient
    protected void finishResolution (ClientObject clobj)
    {
        super.finishResolution(clobj);
        final MemberObject user = (MemberObject)clobj;

        if (!user.isGuest()) {
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

    /**
     * Return true if we're resolving a guest.
     */
    protected boolean isResolvingGuest ()
    {
        // this seems strange, but we're testing the authentication username, which is set to be a
        // MemberName for guests and a regular Name for members. The reason for this is that the
        // guests will use the same MemberName object for their display name and auth name
        return (_username instanceof MemberName);
    }
}
