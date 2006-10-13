//
// $Id$

package com.threerings.msoy.server;

import java.util.ArrayList;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
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
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Photo;
import com.threerings.msoy.web.data.Profile;

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
    public void init (
        MemberRepository memberRepo, ProfileRepository profileRepo)
    {
        _memberRepo = memberRepo;
        _profileRepo = profileRepo;
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
                            log.warning("Unable to update usage " +
                                "from an avatar change.");
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

    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;

    /** Provides access to persistent profile data. */
    protected ProfileRepository _profileRepo;
}
