//
// $Id$

package com.threerings.msoy.server;

import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.persist.MemberRepository;

/**
 * Manage msoy members.
 */
public class MemberManager
    implements MemberProvider
{
    /** The member repository. */
    public MemberRepository memberRepo;

    /**
     * Construct our member manager.
     */
    public MemberManager (MemberRepository memberRepo)
    {
        this.memberRepo = memberRepo;
        MsoyServer.invmgr.registerDispatcher(new MemberDispatcher(this), true);
    }

    /**
     * Loads the specified member's friends list. The results may come from the
     * cache and will be cached if they were loaded from the database.
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
                return memberRepo.getFriends(memberId);
            }
            public void handleSuccess () {
                _friendCache.put(memberId, _result);
                super.handleSuccess();
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
                    _entry = memberRepo.inviteOrApproveFriend(
                        user.getMemberId(), friendId);
                } else {
                    memberRepo.removeFriends(user.getMemberId(), friendId);
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

    /** A soft reference cache of friends lists indexed on memberId. */
    protected SoftCache<Integer,ArrayList<FriendEntry>> _friendCache =
        new SoftCache<Integer,ArrayList<FriendEntry>>();
}
