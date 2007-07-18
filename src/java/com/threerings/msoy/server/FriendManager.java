//
// $Id$

package com.threerings.msoy.server;

import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.ResultListener;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ConfirmAdapter;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import static com.threerings.msoy.Log.log;

/**
 * Handles management of member's friends, including their online status and adding and removing
 * friends.
 */
public class FriendManager
    implements MsoyPeerManager.RemoteMemberObserver
{
    /**
     * Called when a member logs onto this server.
     */
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
                if (MsoyServer.peerMan.locateClient(entry.name) != null) {
                    memobj.updateFriends(new FriendEntry(entry.name, true));
                }
            }
        } finally {
            memobj.commitTransaction();
        }
    }

    /**
     * Called when a member logs off of this server.
     */
    public void memberLoggedOff (MemberObject memobj)
    {
        for (FriendEntry entry : memobj.friends) {
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
     * Called to notify the friend manager that a friendship request was accepted. TODO: make this
     * work via peer services.
     */
    public void friendshipEstablished (MemberName acceptor, MemberName friend)
    {
        // remove them from the friends list of both parties of they are online
        MemberObject accobj = MsoyServer.lookupMember(acceptor.getMemberId());
        MemberObject frobj = MsoyServer.lookupMember(friend.getMemberId());
        if (accobj != null) {
            accobj.addToFriends(new FriendEntry(friend, frobj != null));
        }
        if (frobj != null) {
            frobj.addToFriends(new FriendEntry(acceptor, accobj != null));
        }
    }

    /**
     * Called to notify the friend manager that a friendship was removed. TODO: make this work via
     * peer services.
     */
    public void friendshipCleared (int removerId, int friendId)
    {
        // remove them from the friends list of both parties of they are online
        MemberObject remover = MsoyServer.lookupMember(removerId);
        if (remover != null) {
            remover.removeFromFriends(friendId);
        }
        MemberObject exfriend = MsoyServer.lookupMember(friendId);
        if (exfriend != null) {
            exfriend.removeFromFriends(removerId);
        }
    }

    // from interface MsoyPeerManager.RemoteMemberObserver
    public void remoteMemberLoggedOn (MemberName member)
    {
    }

    // from interface MsoyPeerManager.RemoteMemberObserver
    public void remoteMemberLoggedOff (MemberName member)
    {
    }

    /** A mapping from member id to member object of members on this server that are friends of the
     * member in question. */
    protected HashIntMap<List<MemberObject>> _friendMap = new HashIntMap<List<MemberObject>>();
}
