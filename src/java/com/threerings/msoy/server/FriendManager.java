//
// $Id$

package com.threerings.msoy.server;

import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ResultListener;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ConfirmAdapter;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.FriendInviteObject;

import static com.threerings.msoy.Log.log;

/**
 * Handles management of member's friends, including their online status and adding and removing
 * friends.
 */
public class FriendManager
{
    public void memberLoggedOn (final MemberObject memobj)
    {
        if (memobj.isGuest()) {
            return; // no need to do anything
        }

        // determine which of this member's friends are online
        memobj.startTransaction();
        try {
            FriendEntry[] snapshot = memobj.friends.toArray(new FriendEntry[memobj.friends.size()]);
            for (FriendEntry entry : snapshot) {
                checkAndNotifyFriend(memobj, entry);
            }
        } finally {
            memobj.commitTransaction();
        }
    }

    public void memberLoggedOff (MemberObject memobj)
    {
        for (FriendEntry entry : memobj.friends) {
            // TEMP: sanity check
            if (entry.name.equals(memobj.memberName)) {
                log.warning("Why am I mine own friend? [user=" + memobj.memberName + "].");
                continue;
            }
            // END TEMP

            if (!entry.online) {
                continue;
            }

            // look up online friends..
            MemberObject friendObj = MsoyServer.lookupMember(entry.name);
            if (friendObj == null) {
                log.warning("Online friend not really online? [us=" + memobj.memberName +
                            ", them=" + entry.name + "].");
                continue;
            }
            FriendEntry userEntry = friendObj.friends.get(memobj.getMemberId());
            if (userEntry == null) {
                log.warning("Our friend doesn't know us? [us=" + memobj.memberName +
                            ", them=" + entry.name + "].");
                continue;
            }

            friendObj.startTransaction();
            try {
                // update their friend entry
                userEntry.online = false;
                friendObj.updateFriends(userEntry);
            } finally {
                friendObj.commitTransaction();
            }
        }
    }

    /**
     * Adds or removes the specified friend.
     */
    public void alterFriend (int userId, int friendId, boolean add,
                             ResultListener<Void> listener)
    {
        alterFriend(MsoyServer.lookupMember(userId), userId, friendId, add, listener);
    }

    /**
     * Adds or removes the specified friend.
     */
    public void alterFriend (MemberObject user, int friendId, boolean add,
                             InvocationService.ConfirmListener lner)
        throws InvocationException
    {
        if (add) {
            String subject = MsoyServer.msgMan.getBundle("server").get("m.friend_invite_subject");
            String body = MsoyServer.msgMan.getBundle("server").get("m.friend_invite_body");
            MsoyServer.mailMan.deliverMessage(
                user.memberName.getMemberId(), friendId, subject, body,
                new FriendInviteObject(), new ConfirmAdapter(lner));

        } else {
            alterFriend(user, user.getMemberId(), friendId, add, new ConfirmAdapter(lner));
        }
    }

    /**
     * Generic alterFriend() functionality for the two public methods above. Please note that user
     * can be null here (i.e. offline).
     */
    protected void alterFriend (final MemberObject user, final int userId, final int friendId,
                                final boolean add, ResultListener<Void> lner)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>("alterFriend", lner) {
            public Void invokePersistResult () throws PersistenceException {
                if (add) {
                    _entry = MsoyServer.memberRepo.inviteFriend(userId, friendId);
                    if (user != null) {
                        _userName = user.memberName;
                    } else {
                        _userName = MsoyServer.memberRepo.loadMember(userId).getName();
                    }
                } else {
                    MsoyServer.memberRepo.removeFriends(userId, friendId);
                }
                return null;
            }

            public void handleSuccess () {
                FriendEntry oldEntry = user != null ? user.friends.get(friendId) : null;
                MemberName friendName = (oldEntry != null) ?
                    oldEntry.name : (_entry != null ? _entry.name : null);
                MemberObject friendObj = (friendName != null) ?
                    MsoyServer.lookupMember(friendName) : null;

                // update ourselves and the friend
                if (!add || _entry == null) {
                    // remove the friend
                    if (oldEntry != null) {
                        if (user != null) {
                            user.removeFromFriends(friendId);
                        }
                        if (friendObj != null) {
                            friendObj.removeFromFriends(userId);
                        }
                    }

                } else {
                    // add or update the friend/status
                    _entry.online = (friendObj != null);
                    if (oldEntry == null) {
                        if (user != null) {
                            user.addToFriends(_entry);
                        }
                        if (friendObj != null) {
                            FriendEntry opp = new FriendEntry(_userName, user != null);
                            friendObj.addToFriends(opp);
                        }
                    }
                }
                _listener.requestCompleted(null);
            }

            protected FriendEntry _entry;
            protected MemberName _userName;
        });
    }

    protected void checkAndNotifyFriend (MemberObject memobj, FriendEntry entry)
    {
        // TODO: wire this up to peer services

        // determine whether this friend is online
        MemberObject friendObj = MsoyServer.lookupMember(entry.name);
        if (friendObj == null) {
            return;
        }

        // this friend is online, mark them as such
        memobj.updateFriends(new FriendEntry(entry.name, true));

        // and notify them that we're online (when the account is newly created, my friends won't
        // yet know that i exist)
        if (friendObj.friends.containsKey(memobj.getMemberId())) {
            friendObj.updateFriends(new FriendEntry(memobj.memberName, true));
        }
    }
}
