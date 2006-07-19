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
            ClientObject caller, final Name friend, final boolean add,
            final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MsoyUserObject user = (MsoyUserObject) caller;

        MsoyServer.invoker.postUnit(new Invoker.Unit("alterFriend") {
            public boolean invoke ()
            {
                try {
                    if (add) {
                        _status = memberRepo.inviteOrApproveFriend(
                            user.memberId, friend);

                    } else {
                        memberRepo.removeFriends(user.memberId, friend);
                        _status = (byte) -1;
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

                if (!add || _status == -1) {
                    // remove the friend
                    user.removeFromFriends(friend);

                } else {
                    // add or update the friend/status
                    FriendEntry entry = user.friends.get(friend);
                    if (entry == null) {
                        boolean friendIsOnline = false; // TODO
                        user.addToFriends(
                            new FriendEntry(friend, friendIsOnline, _status));

                    } else {
                        entry.status = _status;
                        user.updateFriends(entry);
                    }
                }
            }

            protected Exception _error;

            protected byte _status;
        });

        throw new InvocationException("TODO");
    }
}
