//
// $Id$

package com.threerings.msoy.server;

import com.samskivert.io.PersistenceException;

import com.samskivert.util.Invoker;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MsoyUserObject;
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

    // from interface MemberProvider
    public void alterFriend (
            ClientObject caller, final int friendId, final boolean add,
            final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MsoyUserObject user = (MsoyUserObject) caller;

        MsoyServer.invoker.postUnit(new Invoker.Unit("alterFriend") {
            public boolean invoke ()
            {
                try {
                    if (add) {
                        _entry = memberRepo.inviteOrApproveFriend(
                            user.memberId, friendId);

                    } else {
                        memberRepo.removeFriends(user.memberId, friendId);
                    }

                } catch (PersistenceException pe) {
                    _error = pe;
                }
                return true;
            }

            public void handleResult ()
            {
                if (_error != null) {
                    listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
                    return;
                }

                FriendEntry oldEntry = user.friends.get(friendId);
                if (!add || _entry == null) {
                    // remove the friend
                    if (oldEntry != null) {
                        user.removeFromFriends(friendId);
                    }

                } else {
                    // add or update the friend/status
                    if (oldEntry == null) {
                        // TODO: real lookup of online status
                        _entry.online = false; // TODO
                        user.addToFriends(_entry);

                    } else {
                        _entry.online = oldEntry.online;
                        user.updateFriends(_entry);
                    }
                }
            }

            protected Exception _error;

            protected FriendEntry _entry;
        });
    }
}
