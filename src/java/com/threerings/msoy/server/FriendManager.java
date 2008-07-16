//
// $Id$

package com.threerings.msoy.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.AnyThread;
import com.threerings.presents.annotation.EventThread;

import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.person.server.persist.ProfileRecord;

import static com.threerings.msoy.Log.log;

/**
 * Handles management of member's friends, including their online status and adding and removing
 * friends.
 */
@Singleton @EventThread
public class FriendManager
    implements MsoyPeerManager.RemoteMemberObserver
{
    /**
     * Called to report that a friendship request was accepted.
     */
    @AnyThread
    public void friendshipEstablished (MemberName acceptor, MemberName friend)
    {
        // add them to the friends list of both parties if/wherever they are online
        _peerMan.invokeNodeAction(new AddFriend(acceptor.getMemberId(), friend));
        _peerMan.invokeNodeAction(new AddFriend(friend.getMemberId(), acceptor));
    }

    /**
     * Called to report that a friendship was removed. May be called from any thread.
     */
    public void friendshipCleared (int removerId, int friendId)
    {
        // remove them from the friends list of both parties, wherever they are online
        _peerMan.invokeNodeAction(new RemoveFriend(removerId, friendId));
        _peerMan.invokeNodeAction(new RemoveFriend(friendId, removerId));
    }

    /**
     * Prepares the friend manager for operation.
     */
    public void init ()
    {
        // register to hear when members log on and off of remote peers
        _peerMan.addRemoteMemberObserver(this);
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
                if (_peerMan.locateClient(entry.name) != null) {
                    memobj.updateFriends(
                        new FriendEntry(entry.name, true, entry.photo, entry.status));
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
        updateOnlineStatus(memobj.getMemberId(), false);
    }

    // from interface MsoyPeerManager.RemoteMemberObserver
    public void remoteMemberLoggedOn (MemberName member)
    {
        updateOnlineStatus(member.getMemberId(), true);
    }

    // from interface MsoyPeerManager.RemoteMemberObserver
    public void remoteMemberLoggedOff (MemberName member)
    {
        updateOnlineStatus(member.getMemberId(), false);
    }

    public void remoteMemberEnteredScene (MemberLocation loc, String hostname, int port)
    {
        // nada
    }

    protected void registerFriendInterest (MemberObject memobj, int friendId)
    {
        _friendMap.put(friendId, memobj);
    }

    protected void clearFriendInterest (MemberObject memobj, int friendId)
    {
        if (!_friendMap.remove(friendId, memobj)) {
            log.warning("Watcher not listed when interest cleared? [watcher=" + memobj.who() +
                        ", friend=" + friendId + "].");
        }
    }

    protected void updateOnlineStatus (int memberId, boolean online)
    {
        // TODO: add filter to avoid off/on when member switches servers

        for (MemberObject watcher : _friendMap.get(memberId)) {
            FriendEntry entry = watcher.friends.get(memberId);
            if (entry == null) {
                log.warning("Missing entry for registered watcher? [watcher=" + watcher.who() +
                            ", friend=" + memberId + "].");
                continue;
            }
            watcher.updateFriends(new FriendEntry(entry.name, online, entry.photo, entry.status));
        }
    }

    protected static class AddFriend extends MemberNodeAction
    {
        public AddFriend (int memberId, MemberName friend) {
            super(memberId);
            _friendId = friend.getMemberId();
            _friendName = friend.toString();
        }

        protected void execute (final MemberObject memobj) {
            final MemberName friend = new MemberName(_friendName, _friendId);
            final boolean online = (MsoyServer.peerMan.locateClient(friend) != null);
            MsoyServer.invoker.postUnit(new Invoker.Unit("AddFriend") {
                public boolean invoke () {
                    try {
                        ProfileRecord profile = MsoyServer.profileRepo.loadProfile(_friendId);
                        _photo = profile.getPhoto();
                        _status = profile.headline;
                    } catch (PersistenceException pe) {
                        log.warning("Failed to fetch profile info for new friend", "friendId", 
                            _friendId, "exception", pe);
                    }
                    // even if we hit an exception, we still want to continue back on the dobj 
                    // thread
                    return true;
                }
                public void handleResult () {
                    memobj.addToFriends(new FriendEntry(friend, online, _photo, _status));
                    MsoyServer.friendMan.registerFriendInterest(memobj, _friendId);
                }
                protected MediaDesc _photo = null;
                protected String _status = "";
            });
        }

        protected int _friendId;
        protected String _friendName;
    }

    protected static class RemoveFriend extends MemberNodeAction
    {
        public RemoveFriend (int memberId, int friendId) {
            super(memberId);
            _friendId = friendId;
        }

        protected void execute (MemberObject memobj) {
            memobj.removeFromFriends(_friendId);
            MsoyServer.friendMan.clearFriendInterest(memobj, _friendId);
        }

        protected int _friendId;
    }

    /** A mapping from member id to the member objects of members on this server that are friends
     * of the member in question. */
    protected Multimap<Integer,MemberObject> _friendMap = new HashMultimap<Integer,MemberObject>();

    @Inject protected MsoyPeerManager _peerMan;
}
