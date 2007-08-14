//
// $Id$

package com.threerings.msoy.server;

import java.util.ArrayList;
import java.util.List;
import com.samskivert.util.HashIntMap;
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
     * Prepares the friend manager for operation.
     */
    public void init ()
    {
        // register to hear when members log on and off of remote peers
        MsoyServer.peerMan.addRemoteMemberObserver(this);
    }

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
                registerFriendInterest(memobj, entry.name.getMemberId());
            }
        } finally {
            memobj.commitTransaction();
        }

        // let local friends know this member is online
        updateOnlineStatus(memobj.getMemberId(), true);
    }

    /**
     * Called when a member logs off of this server.
     */
    public void memberLoggedOff (MemberObject memobj)
    {
        // clear out our friend interest registrations
        for (FriendEntry entry : memobj.friends) {
            clearFriendInterest(memobj, entry.name.getMemberId());
        }

        // let local friends know this member is offline
        updateOnlineStatus(memobj.getMemberId(), true);
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
            registerFriendInterest(accobj, friend.getMemberId());
        }
        if (frobj != null) {
            frobj.addToFriends(new FriendEntry(acceptor, accobj != null));
            registerFriendInterest(frobj, acceptor.getMemberId());
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
            clearFriendInterest(remover, friendId);
        }
        MemberObject exfriend = MsoyServer.lookupMember(friendId);
        if (exfriend != null) {
            exfriend.removeFromFriends(removerId);
            clearFriendInterest(exfriend, removerId);
        }
    }

    // from interface MsoyPeerManager.RemoteMemberObserver
    public void remoteMemberLoggedOn (MemberName member)
    {
        // TODO: handle server switches
        updateOnlineStatus(member.getMemberId(), true);
    }

    // from interface MsoyPeerManager.RemoteMemberObserver
    public void remoteMemberLoggedOff (MemberName member)
    {
        // TODO: handle server switches
        updateOnlineStatus(member.getMemberId(), false);
    }

    protected void registerFriendInterest (MemberObject memobj, int friendId)
    {
        List<MemberObject> watchers = _friendMap.get(friendId);
        if (watchers == null) {
            _friendMap.put(friendId, watchers = new ArrayList<MemberObject>());
        }
        watchers.add(memobj);
    }

    protected void clearFriendInterest (MemberObject memobj, int friendId)
    {
        List<MemberObject> watchers = _friendMap.get(friendId);
        if (watchers == null) {
            log.warning("No watchers list for cleared friend interest? [watcher=" + memobj.who() +
                        ", friend=" + friendId + "].");
            return;
        }
        if (!watchers.remove(memobj)) {
            log.warning("Watcher not listed when interest cleared? [watcher=" + memobj.who() +
                        ", friend=" + friendId + "].");
        }
        if (watchers.size() == 0) {
            _friendMap.remove(friendId);
        }
    }

    protected void updateOnlineStatus (int memberId, boolean online)
    {
        List<MemberObject> watchers = _friendMap.get(memberId);
        if (watchers == null) {
            return; // alas, this member is unpopular and has no online friends
        }
        for (MemberObject watcher : watchers) {
            FriendEntry entry = watcher.friends.get(memberId);
            if (entry == null) {
                log.warning("Missing entry for registered watcher? [watcher=" + watcher.who() +
                            ", friend=" + memberId + "].");
                continue;
            }
            watcher.updateFriends(new FriendEntry(entry.name, online));
        }
    }

    /** A mapping from member id to member object of members on this server that are friends of the
     * member in question. */
    protected HashIntMap<List<MemberObject>> _friendMap = new HashIntMap<List<MemberObject>>();
}
