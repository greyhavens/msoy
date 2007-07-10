//
// $Id$

package com.threerings.msoy.server;

import java.util.ArrayList;
import java.util.List;

import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.SceneBookmarkEntry;
import com.threerings.msoy.person.server.persist.MailRepository;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;

import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.world.data.MsoySceneModel;

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
        // set up the standard user access controller
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
        _avatarId = member.avatarId;

        // configure their member name which is a combination of their display name and their
        // member id
        userObj.setMemberName(new MemberName(member.name, member.memberId));
        userObj.setHomeSceneId(member.homeSceneId);

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

        userObj.setFlow(member.flow);
        userObj.setAccFlow(member.accFlow);

        userObj.setLevel(member.level);

        userObj.setHumanity(member.humanity);
        userObj.setOwnedScenes(new DSet<SceneBookmarkEntry>(
            MsoyServer.sceneRepo.getOwnedScenes(
                MsoySceneModel.OWNER_TYPE_MEMBER, member.memberId).iterator()));
        ArrayList<GroupMembership> groups = new ArrayList<GroupMembership>();
        for (GroupMembershipRecord record : MsoyServer.groupRepo.getMemberships(member.memberId)) {
            GroupRecord group = MsoyServer.groupRepo.loadGroup(record.groupId);
            if (group == null) {
                log.warning("User member of non-existent group?! [who=" + member.accountName +
                            ", groupId=" + record.groupId + "].");
                continue;
            }
            groups.add(record.toGroupMembership(group, null));
        }
        userObj.setGroups(new DSet<GroupMembership>(groups.iterator()));

        MailRepository mailRepo = MsoyServer.mailMan.getRepository();
        Tuple<Integer, Integer> count = mailRepo.getMessageCount(
            member.memberId, MailFolder.INBOX_FOLDER_ID);
        userObj.setHasNewMail(count.right > 0);
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

        // load up their friend info
        if (!user.isGuest()) {
            MsoyServer.memberMan.loadFriends(user.getMemberId(),
                new ResultListener<List<FriendEntry>>() {
                public void requestCompleted (List<FriendEntry> friends) {
                    finishInitFriends(user, friends);
                }
                public void requestFailed (Exception cause) {
                    log.warning("Failed to load member's friend info [who=" + user.who() +
                                ", error=" + cause + "].");
                }
            });

            MsoyServer.itemMan.loadRecentlyTouched(user.getMemberId(), Item.AVATAR,
                MemberObject.AVATAR_CACHE_SIZE, new ResultListener<ArrayList<Item>>() {
                    public void requestCompleted (ArrayList<Item> items) {
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

            if (_avatarId != 0) {
                MsoyServer.itemMan.getItem(new ItemIdent(Item.AVATAR, _avatarId),
                                           new ResultListener<Item>() {
                    public void requestCompleted (Item avatar) {
                        user.setAvatar((Avatar) avatar);
                        MsoyServer.memberMan.updateOccupantInfo(user);
                    }
                    public void requestFailed (Exception cause) {
                        log.warning("Failed to load member's avatar [who=" + user.who() +
                                    ", error=" + cause + "].");
                    }
                });
            }
        }
    }

    /**
     * Called from {@link #finishResolution} once our friends have been loaded.
     */
    protected void finishInitFriends (MemberObject user, List<FriendEntry> friends)
    {
        for (FriendEntry entry : friends) {
            MemberObject friendObj = MsoyServer.lookupMember(entry.name);
            if (friendObj == null) {
                continue;
            }
            // this friend is online, mark them as such
            entry.online = true;
            // and notify them that we're online
            FriendEntry userEntry = friendObj.friends.get(user.getMemberId());
            // when the account is newly created, my friends won't yet know that i exist
            if (userEntry != null) {
                userEntry.online = true;
                friendObj.updateFriends(userEntry);
            }
        }
        user.setFriends(new DSet<FriendEntry>(friends.iterator()));
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

    /** The user's avatarId, or 0. */
    protected int _avatarId;
}
