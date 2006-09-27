//
// $Id$

package com.threerings.msoy.server;

import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;
import com.samskivert.util.StringUtil;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Photo;
import com.threerings.msoy.web.data.Profile;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.ProfileRepository;

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
     * Loads the specified member's friends list. The results may come from the
     * cache and will be cached if they are loaded from the database.
     */
    public void loadFriends (
        final int memberId, ResultListener<ArrayList<FriendEntry>> listener)
    {
        // first check the cache
        ArrayList<FriendEntry> friends = _friendCache.get(memberId);
        if (friends != null) {
            listener.requestCompleted(friends);
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<FriendEntry>>(listener) {
            public ArrayList<FriendEntry> invokePersistResult ()
                throws PersistenceException {
                return _memberRepo.getFriends(memberId);
            }
            public void handleSuccess () {
                _friendCache.put(memberId, _result);
                super.handleSuccess();
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
                profile.photo.photoMediaHash = StringUtil.unhexlate(
                    "816cd5aebc2d9d228bf66cff193b81eba1a6ac85");
                profile.photo.photoMimeType = MediaDesc.IMAGE_JPEG;
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

    // from interface MemberProvider
    public void alterFriend (
        ClientObject caller, final int friendId, final boolean add,
        InvocationService.InvocationListener lner)
            throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
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
                if (!add || _entry == null) {
                    // remove the friend
                    if (oldEntry != null) {
                        user.removeFromFriends(friendId);
                    }

                } else {
                    // add or update the friend/status
                    _entry.online =
                        (MsoyServer.lookupMember(_entry.name) != null);
                    if (oldEntry == null) {
                        user.addToFriends(_entry);
                    } else {
                        user.updateFriends(_entry);
                    }
                }

                // keep the cache up to date
                ArrayList<FriendEntry> flist =
                    _friendCache.get(user.getMemberId());
                if (flist != null) {
                    if (oldEntry != null) {
                        flist.remove(oldEntry);
                    }
                    if (_entry != null) {
                        flist.add(_entry);
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
        // TODO: only give out homeIds to people who are friends?
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

    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;

    /** Provides access to persistent profile data. */
    protected ProfileRepository _profileRepo;

    /** A soft reference cache of friends lists indexed on memberId. */
    protected SoftCache<Integer,ArrayList<FriendEntry>> _friendCache =
        new SoftCache<Integer,ArrayList<FriendEntry>>();
}
