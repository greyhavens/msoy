//
// $Id$

package com.threerings.msoy.world.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.ShutdownManager.Shutdowner;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.server.WorldGameRegistry;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.peer.server.MsoyPeerManager.MemberObserver;
import com.threerings.msoy.world.client.WatcherReceiver;

import static com.threerings.msoy.Log.log;

/**
 * Maintains a list of watched members whose movements from scene to scene anywhere in the
 * Whirled (i.e. cross server) are reported through the {@link WatcherReceiver} facility.
 *
 * The sole client of this service is currently the game server, allowing AVRGs to keep track
 * of what rooms their various current players are in.
 */
@Singleton @EventThread
public class WorldWatcherManager
    implements MemberObserver, Shutdowner, WatcherProvider
{
    @Inject public WorldWatcherManager (
        ShutdownManager shutmgr, InvocationManager invmgr, MsoyPeerManager peermgr)
    {
        shutmgr.registerShutdowner(this);
        peermgr.memberObs.add(this);
        invmgr.registerDispatcher(new WatcherDispatcher(this), WorldGameRegistry.GAME_SERVER_GROUP);
    }

    // from interface MemberObserver
    public void memberLoggedOff (String node, MemberName member)
    {
        ClientObject watcher = _memberWatchers.get(member.getMemberId());
        log.debug("Remote member logged off", "member", member, "watcher", watcher);
        if (watcher != null) {
            WatcherSender.memberLoggedOff(watcher, member.getMemberId());
        }
    }

    // from interface MemberObserver
    public void memberLoggedOn (String node, MemberName member)
    {
        // nada
    }

    // from interface .MemberObserver
    public void memberEnteredScene (String node, MemberLocation loc)
    {
        ClientObject watcher = _memberWatchers.get(loc.memberId);
        String host = _peerMgr.getPeerPublicHostName(node);
        int port = _peerMgr.getPeerPort(node);
        log.debug("Remote member entered scene", "loc", loc, "node", node, "watcher", watcher);
        
        if (watcher != null) {
            WatcherSender.memberMoved(watcher, loc.memberId, loc.sceneId, host, port);
        }
    }

    // from interface Shutdowner
    public void shutdown ()
    {
        // nothing at the moment
    }

    // from interface WatcherProvider
    public void addWatch (ClientObject caller, int memberId)
    {
        if (_memberWatchers.containsKey(memberId)) {
            log.warning("Discarding existing watcher", "memberId", memberId);
        }

        // add a death listener to this watcher if we've not already done so
        if (!_watcherMembers.containsKey(caller.getOid())) {
            caller.addListener(_watcherCleanup);
        }

        _memberWatchers.put(memberId, caller);
        _watcherMembers.put(caller.getOid(), memberId);
        
        MemberLocation location = _peerMgr.getMemberLocation(memberId);
        if (location == null) {
            log.warning("Watched member has no current location", "memberId", memberId);
            return;
        }

        Tuple<String, HostedRoom> room = _peerMgr.getSceneHost(location.sceneId);
        if (room == null) {
            log.warning("Host not found for scene", "location", location);
            return;
        }
        
        memberEnteredScene(room.left, location);
    }

    // from interface WatcherProvider
    public void clearWatch (ClientObject caller, int memberId)
    {
        if (!_memberWatchers.containsKey(memberId)) {
            log.warning("Attempting to clear nonexistent watch", "memberId", memberId);
            return;
        }
        _memberWatchers.remove(memberId);
        _watcherMembers.remove(caller.getOid(), memberId);

        // if this was the last member being watched, unsubscribe to death events
        if (!_watcherMembers.containsKey(caller.getOid())) {
            caller.removeListener(_watcherCleanup);
        }
    }

    protected ObjectDeathListener _watcherCleanup = new ObjectDeathListener () {
        public void objectDestroyed (ObjectDestroyedEvent event) {
            int watcherOid = event.getTargetOid();
            log.debug("Flushing disconnected movement watcher", "oid", watcherOid);
            for (int member : _watcherMembers.get(watcherOid)) {
                _memberWatchers.remove(member);
            }
            _watcherMembers.removeAll(watcherOid);
        }
    };

    /** A map of each memberId that we're watching to the (one) caller to notify of the move. */
    protected IntMap<ClientObject> _memberWatchers = new HashIntMap<ClientObject>();

    /** A multimap of caller Oid to memberId that can easily tell us who's watched by whom. */
    protected Multimap<Integer, Integer> _watcherMembers = new HashMultimap<Integer, Integer>();
    
    // Dependencies
    @Inject MsoyPeerManager _peerMgr;
}
