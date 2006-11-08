//
// $Id$

package com.threerings.msoy.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MemberName;
import com.threerings.msoy.data.SceneBookmarkEntry;
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Photo;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MemberGName;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.GroupRepository;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.ProfileRepository;

import static com.threerings.msoy.Log.log;

/**
 * Manage msoy members.
 */
public class MemberManager
    implements MemberProvider
{
    /**
     * Prepares our member manager for operation.
     */
    public void init (MemberRepository memberRepo, ProfileRepository profileRepo,
                      GroupRepository groupRepo)
    {
        _memberRepo = memberRepo;
        _profileRepo = profileRepo;
        _groupRepo = groupRepo;
        MsoyServer.invmgr.registerDispatcher(new MemberDispatcher(this), true);
    }

    /**
     * Loads the specified member's friends list. 
     *
     * Note: all the friends will be marked as offline. If you
     * desire to know their online status, that should be filled in
     * elsewhere.
     */
    public void loadFriends (
        final int memberId, ResultListener<ArrayList<FriendEntry>> listener)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<FriendEntry>>(listener) {
            public ArrayList<FriendEntry> invokePersistResult ()
                throws PersistenceException {
                return _memberRepo.getFriends(memberId);
            }
        });
    }

    /**
     * Loads the specified member's profile.
     */
    public void loadProfile (
        final int memberId, ResultListener<Profile> listener)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Profile>(listener) {
            public Profile invokePersistResult () throws PersistenceException {
                // load up their member info
                MemberRecord member = _memberRepo.loadMember(memberId);
                if (member == null) {
                    return null;
                }

                Profile profile = new Profile();
                profile.memberId = memberId;
                profile.displayName = member.name;
                // profile.lastLogon = ;

                // fake bits!
                profile.photo = new Photo();
                profile.photo.photoMedia =
                    new MediaDesc(StringUtil.unhexlate(
                                      "816cd5aebc2d9d228bf66cff193b81eba1a6ac85"), MediaDesc.IMAGE_JPEG);
                profile.headline = "Arr! Mateys, this here be me profile!";
                profile.homePageURL = "http://www.puzzlepirates.com/";
                profile.isMale = true;
                profile.location = "San Francisco, CA";
                profile.age = 36;

//                 ProfileRecord prec = _profileRepo.loadProfile(memberId);
//                 if (prec != null) {
//                     profile.

                // load other bits!
                return profile;
            }
        });
    }

    /**
     * Look up a member's record and construct a MemberGName from it.
     */
    public void getName (final int memberId, ServletWaiter<MemberGName> waiter)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<MemberGName>(waiter) {
            public MemberGName invokePersistResult () throws PersistenceException {
                return new MemberGName(_memberRepo.loadMember(memberId).name, memberId);
            }
        });
    }
    /**
     * Update the user's occupant info.
     */
    public void updateOccupantInfo (MemberObject user)
    {
        PlaceManager pmgr = MsoyServer.plreg.getPlaceManager(user.location);
        if (pmgr != null) {
            pmgr.updateOccupantInfo(user.createOccupantInfo());
        }
    }

    // from interface MemberProvider
    public void alterFriend (
        ClientObject caller, final int friendId, final boolean add,
        InvocationService.InvocationListener lner)
            throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        MsoyServer.invoker.postUnit(new PersistingUnit("alterFriend", lner) {
            public void invokePersistent () throws PersistenceException {
                if (add) {
                    _entry = _memberRepo.inviteOrApproveFriend(
                        user.getMemberId(), friendId);
                } else {
                    _memberRepo.removeFriends(user.getMemberId(), friendId);
                }
            }

            public void handleSuccess () {
                FriendEntry oldEntry = user.friends.get(friendId);
                MemberName friendName = (oldEntry != null) ? oldEntry.name :
                    (_entry != null ? _entry.name : null);
                MemberObject friendObj = (friendName != null)
                    ? MsoyServer.lookupMember(friendName) : null;

                // update ourselves and the friend
                if (!add || _entry == null) {
                    // remove the friend
                    if (oldEntry != null) {
                        user.removeFromFriends(friendId);
                        if (friendObj != null) {
                            friendObj.removeFromFriends(user.getMemberId());
                        }
                    }

                } else {
                    // add or update the friend/status
                    _entry.online = (friendObj != null);
                    byte oppStatus = getOppositeFriendStatus(_entry.status);
                    if (oldEntry == null) {
                        user.addToFriends(_entry);
                        if (friendObj != null) {
                            FriendEntry opp = new FriendEntry(
                                user.memberName, true, oppStatus);
                            friendObj.addToFriends(opp);
                        }

                    } else {
                        user.updateFriends(_entry);
                        if (friendObj != null) {
                            FriendEntry opp = friendObj.friends.get(
                                user.getMemberId());
                            opp.status = oppStatus;
                            friendObj.updateFriends(opp);
                        }
                    }
                }
            }

            protected FriendEntry _entry;
        });
    }

    // from interface MemberProvider
    public void getMemberHomeId (
        ClientObject caller, final int memberId,
        InvocationService.ResultListener listener)
            throws InvocationException
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Integer>(
                new ResultAdapter<Integer>(listener)) {
                public Integer invokePersistResult ()
                throws PersistenceException
                {
                    // load up their member info
                    MemberRecord member = _memberRepo.loadMember(memberId);
                    return (member == null) ? null : member.homeSceneId;
                }

                public void handleSuccess ()
                {
                    if (_result == null) {
                        handleFailure(
                            new InvocationException("m.no_such_user"));
                    } else {
                        super.handleSuccess();
                    }
                }
        });
    }

    // from interface MemberProvider
    public void setAvatar (
        ClientObject caller, int avatarItemId,
        final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        MsoyServer.itemMan.getItem(new ItemIdent(Item.AVATAR, avatarItemId),
            new ResultListener<Item>() {
            public void requestCompleted (Item item)
            {
                Avatar avatar = (Avatar) item;
                finishSetAvatar(user, avatar, listener);
            }

            public void requestFailed (Exception cause)
            {
                log.log(Level.WARNING,
                    "Unable to retrieve user's avatar " +
                    "[cause=" + cause + "].",
                    cause);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // from interface MemberProvider
    public void setDisplayName (
        ClientObject caller, final String name,
        final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        // TODO: verify entered string

        MsoyServer.invoker.postUnit(new RepositoryUnit("setDisplayName") {
            public void invokePersist ()
                throws PersistenceException
            {
                _memberRepo.configureDisplayName(user.getMemberId(), name);
            }

            public void handleSuccess ()
            {
                user.setMemberName(new MemberName(name, user.getMemberId()));
                updateOccupantInfo(user);
            }

            public void handleFailure (Exception pe)
            {
                log.warning("Unable to set display name " +
                    "[user=" + user.which() + ", name='" + name + "', " +
                    "error=" + pe + "].");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // from interface MemberProvider
    public void purchaseRoom (
        ClientObject caller, final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);
        final int memberId = user.getMemberId();
        final String roomName = user.memberName + "'s new room";

        // TODO: charge some flow

        MsoyServer.invoker.postUnit(new RepositoryUnit("purchaseRoom") {
            public void invokePersist ()
                throws PersistenceException
            {
                _newRoomId =
                    MsoyServer.sceneRepo.createBlankRoom(memberId, roomName);
            }

            public void handleSuccess ()
            {
                user.addToOwnedScenes(
                    new SceneBookmarkEntry(_newRoomId, roomName, 0));
                listener.requestProcessed();
            }

            public void handleFailure (Exception pe)
            {
                log.warning("Unable to create a new room " +
                    "[user=" + user.which() + "error=" + pe + "].");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }

            protected int _newRoomId;
        });
    }

    /**
     * Convenience method to ensure that the specified caller is not
     * a guest.
     */
    protected void ensureNotGuest (MemberObject caller)
        throws InvocationException
    {
        if (caller.isGuest()) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
    }

    /**
     * Return the status of a friendship as viewed from the other side.
     */
    protected byte getOppositeFriendStatus (byte status)
    {
        switch (status) {
        case FriendEntry.PENDING_MY_APPROVAL:
            return FriendEntry.PENDING_THEIR_APPROVAL;

        case FriendEntry.PENDING_THEIR_APPROVAL:
            return FriendEntry.PENDING_MY_APPROVAL;

        default:
            return FriendEntry.FRIEND;
        }
    }

    /**
     * Finish configuring the user's avatar.
     */
    protected void finishSetAvatar (
        final MemberObject user, final Avatar avatar,
        final InvocationService.InvocationListener listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("setAvatarPt2") {
            public void invokePersist ()
                throws PersistenceException
            {
                _memberRepo.configureAvatarId(user.getMemberId(), avatar.itemId);
            }

            public void handleSuccess ()
            {
                MsoyServer.itemMan.updateItemUsage(user.getMemberId(),
                    user.avatar, avatar, new ResultListener() {
                        public void requestCompleted (Object result) {}
                        public void requestFailed (Exception cause) {
                            log.warning("Unable to update usage from an avatar change.");
                        }
                    });
                user.setAvatar(avatar);
                updateOccupantInfo(user);
            }

            public void handleFailure (Exception pe)
            {
                log.warning("Unable to set avatar " +
                    "[user=" + user.which() + ", avatar='" + avatar + "', " +
                    "error=" + pe + "].");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });

    }
    
    /**
     * Fetches all group records and sends them back in web object format.
     * This method will most likely become a pager in the near future, or we
     * will be returning some kind of summary object.
     */
    public void getGroups (ResultListener<List<Group>> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<Group>>(listener) {
            public List<Group> invokePersistResult () throws PersistenceException {
                List<Group> groups = new ArrayList<Group>();
                for (GroupRecord gRec : _groupRepo.findGroups()) {
                    groups.add(gRec.toWebObject());
                }
                return groups;
            }
        });
    }

    /**
     * Creates a new group record in the database and return a {@link Group} for
     * it. This method assigns the group a new, unique id.
     * 
     * TODO: Sanity checks on group name.
     */
    public void createGroup (final Group groupDef, ResultListener<Group> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Group>(listener) {
            public Group invokePersistResult () throws PersistenceException {
                GroupRecord gRec = new GroupRecord();
                gRec.name = groupDef.name;
                gRec.charter = groupDef.charter;
                if (groupDef.logo != null) {
                    gRec.logoMimeType = groupDef.logo.mimeType;
                    gRec.logoMediaHash = groupDef.logo.hash;
                }
                gRec.creatorId = groupDef.creatorId;
                gRec.creationDate = new Timestamp(groupDef.creationDate.getTime());
                gRec.policy = groupDef.policy;
                _groupRepo.createGroup(gRec);
    
                _groupRepo.joinGroup(gRec.groupId, gRec.creatorId, GroupMembership.RANK_MANAGER);
    
                return gRec.toWebObject();
            }
        });
    }

    /**
     * Updates a group record in the database with new data. Only non-null/non-zero parameters
     * are used for the update, and data is not read back from the database. This is a low-level
     * method without privilige checks; it's up to the callers to secure it.
     */
    public void updateGroup (final int groupId, final String name, final String charter,
                             final MediaDesc logo, final byte policy,
                             ResultListener<Void> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(listener) {
            public Void invokePersistResult () throws PersistenceException {
                List<Object> argList = new ArrayList<Object>();
                if (name != null) {
                    argList.add(GroupRecord.NAME);
                    argList.add(name);
                }
                if (charter != null) {
                    argList.add(GroupRecord.CHARTER);
                    argList.add(charter);
                }
                if (logo != null) {
                    argList.add(GroupRecord.LOGO_MIME_TYPE);
                    argList.add(logo.mimeType);
                    argList.add(GroupRecord.LOGO_MEDIA_HASH);
                    argList.add(logo.hash);
                }
                if (policy > 0) {
                    argList.add(GroupRecord.POLICY);
                    argList.add(policy);
                }
                _groupRepo.updateGroup(groupId, argList.toArray());
                return null;
            }
        });
    }

    /**
     * Fetches the members of a given group, as {@link GroupMembership} records. This method
     * does not distinguish between a nonexistent group and a group without members;
     * both situations yield empty collections.
     */
    public void getGroupDetail (final int groupId, ResultListener<GroupDetail> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<GroupDetail>(listener) {
            public GroupDetail invokePersistResult () throws PersistenceException {
                // load the group record
                GroupRecord gRec = _groupRepo.loadGroup(groupId);
                // load the creator's member record
                MemberRecord mRec = _memberRepo.loadMember(gRec.creatorId);
                // set up the detail
                GroupDetail detail = new GroupDetail();
                detail.creator = new MemberGName(mRec.name, mRec.memberId);
                detail.group = gRec.toWebObject();
                detail.members = new HashMap<MemberGName, Byte>(); 
                for (GroupMembershipRecord gmRec : _groupRepo.getMembers(groupId)) {
                    mRec = _memberRepo.loadMember(gmRec.memberId);
                    detail.members.put(new MemberGName(mRec.name, mRec.memberId), gmRec.rank);
                }
                return detail;
            }
        });
    }

    /**
     * Fetches the groups a given person is a member of, as {@link GroupMembership} records.
     * This method does not distinguish between a nonexistent person, and a person who is
     * a member of no groups; both situations yield empty collections.
     */
    public void getMembershipGroups (final int memberId, final boolean canInvite,
                                     ResultListener<List<GroupMembership>> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<GroupMembership>>(listener) {
            public List<GroupMembership> invokePersistResult () throws PersistenceException {
                MemberRecord mRec = _memberRepo.loadMember(memberId);
                List<GroupMembership> result = new ArrayList<GroupMembership>();
                for (GroupMembershipRecord gmRec : _groupRepo.getGroups(memberId)) {
                    GroupRecord gRec = _groupRepo.loadGroup(gmRec.groupId);
                    // if we're only including groups we can invite to, strip out exclusive
                    // groups of which we're not managers
                    if (canInvite && gRec.policy == Group.POLICY_EXCLUSIVE &&
                            gmRec.rank != GroupMembership.RANK_MANAGER) {
                        continue;
                    }
                    GroupMembership gm = new GroupMembership();
                    gm.member = new MemberGName(mRec.name, mRec.memberId);
                    gm.groupId = gmRec.groupId;
                    gm.groupName = gRec.name;
                    result.add(gm);
                }
                return result;
            }
        });
    }
    
    /**
     * Cancels the given person's membership in the given group.
     */
    public void leaveGroup (final int groupId, final int memberId, ResultListener<Void> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(listener) {
            public Void invokePersistResult() throws PersistenceException {
                _groupRepo.leaveGroup(groupId, memberId);
                return null;
            }
        });
    }

    /**
     * Makes the given person a member of the given group, of the given rank.
     */
    public void joinGroup (final int groupId, final int memberId, final byte rank,
                           ResultListener<Void> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(listener) {
            public Void invokePersistResult() throws PersistenceException {
                _groupRepo.joinGroup(groupId, memberId, rank);
                return null;
            }
        });        
    }

    /**
     * Returns the rank of a group member, or null.
     */
    public void getRank (final int groupId, final int memberId, ResultListener<Byte> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Byte>(listener) {
            public Byte invokePersistResult() throws PersistenceException {
                GroupMembershipRecord gmr = _groupRepo.getMembership(groupId, memberId);
                return gmr != null ? gmr.rank : null;
            }
        });
    }

    /**
     * Sets the rank of a group member. Throws an exception if there is no
     * such person, no such group, or the person is not a member of the group.
     */
    public void setRank (final int groupId, final int memberId, final byte newRank,
                         ResultListener<Void> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(listener) {
            public Void invokePersistResult() throws PersistenceException {
                _groupRepo.setRank(groupId, memberId, newRank);
                return null;
            }
        });
    }
    
    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;

    /** Provides access to persistent profile data. */
    protected ProfileRepository _profileRepo;
    
    /** Provides access to persistent group data. */
    protected GroupRepository _groupRepo;
}
