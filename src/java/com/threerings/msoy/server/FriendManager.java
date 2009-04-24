//
// $Id$

package com.threerings.msoy.server;

import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.annotation.EventThread;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import static com.threerings.msoy.Log.log;

/**
 * Handles management of member's friends, including their online status and adding and removing
 * friends.
 */
@Singleton @EventThread
public class FriendManager
    implements MemberLocator.Observer, MsoyPeerManager.MemberObserver
{
    @Inject public FriendManager (MemberLocator locator)
    {
        // register to hear about member logon and logoff
        locator.addObserver(this);
    }

    /**
     * Prepares the friend manager for operation.
     */
    public void init ()
    {
        // register to hear when members log on and off of remote peers
        _peerMan.memberObs.add(this);
    }

    // from interface MemberLocator.Observer
    public void memberLoggedOn (final MemberObject memobj)
    {
        // register interest in updates for this member's friends
        Set<Integer> friendIds = Sets.newHashSet();
        FriendEntry[] snapshot = memobj.friends.toArray(new FriendEntry[memobj.friends.size()]);
        for (FriendEntry entry : snapshot) {
            friendIds.add(entry.name.getMemberId());
            registerFriendInterest(memobj, entry.name.getMemberId());
        }

        // determine which are online and update their friend status
        Set<Integer> onlineIds = _peerMan.filterOnline(friendIds);
        memobj.startTransaction();
        try {
            for (FriendEntry entry : snapshot) {
                if (entry.online != onlineIds.contains(entry.name.getMemberId())) {
                    memobj.updateFriends(entry.toggleOnline());
                }
            }
        } finally {
            memobj.commitTransaction();
        }
    }

    // from interface MemberLocator.Observer
    public void memberLoggedOff (MemberObject memobj)
    {
        // clear out our friend interest registrations
        for (FriendEntry entry : memobj.friends) {
            clearFriendInterest(memobj, entry.name.getMemberId());
        }
    }

    // from interface MsoyPeerManager.MemberObserver
    public void memberLoggedOn (String nodeName, MemberName member)
    {
        updateOnlineStatus(member.getMemberId(), true);
    }

    // from interface MsoyPeerManager.MemberObserver
    public void memberLoggedOff (String nodeName, MemberName member)
    {
        updateOnlineStatus(member.getMemberId(), false);
    }

    // from interface MsoyPeerManager.MemberObserver
    public void memberEnteredScene (String nodeName, int memberId, int sceneId)
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
        for (MemberObject watcher : _friendMap.get(memberId)) {
            FriendEntry entry = watcher.friends.get(memberId);
            if (entry == null) {
                log.warning("Missing entry for registered watcher? [watcher=" + watcher.who() +
                            ", friend=" + memberId + "].");
                continue;
            }
            if (entry.online != online) {
                watcher.updateFriends(entry.toggleOnline());
            }
        }
    }

    /** A mapping from member id to the member objects of members on this server that are friends
     * of the member in question. */
    protected Multimap<Integer,MemberObject> _friendMap = HashMultimap.create();

    @Inject protected MsoyPeerManager _peerMan;
}
