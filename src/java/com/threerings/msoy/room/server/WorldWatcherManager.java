//
// $Id: PetManager.java 9626 2008-06-29 14:08:13Z mdb $

package com.threerings.msoy.room.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Tuple;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.server.MsoyGameRegistry;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.peer.server.MsoyPeerManager.RemoteMemberObserver;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.ShutdownManager.Shutdowner;

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
    implements RemoteMemberObserver, Shutdowner, WatcherProvider
{
    @Inject public WorldWatcherManager (
        ShutdownManager shutmgr, InvocationManager invmgr, MsoyPeerManager peermgr)
    {
        shutmgr.registerShutdowner(this);
        peermgr.addRemoteMemberObserver(this);
        invmgr.registerDispatcher(new WatcherDispatcher(this), MsoyGameRegistry.GAME_SERVER_GROUP);
    }

    // from interface RemoteMemberObserver
    public void remoteMemberLoggedOff (MemberName member)
    {
        // nada
    }

    // from interface RemoteMemberObserver
    public void remoteMemberLoggedOn (MemberName member)
    {
        // nada
    }

    // from interface RemoteMemberObserver
    public void remoteMemberEnteredScene (MemberLocation loc, String hostname, int port)
    {
        ClientObject watcher = _memberWatchers.get(loc.memberId);
        log.info(
            "remoteMemberEnteredScene", "loc", loc, "hostname", hostname, "port", port, 
            "watcher", watcher);
        if (watcher != null) {
            WatcherSender.memberMoved(watcher, loc.memberId, loc.sceneId, hostname, port);
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
        
        String host = _peerMgr.getPeerPublicHostName(room.left);
        int port = _peerMgr.getPeerPort(room.left);
        remoteMemberEnteredScene(location, host, port);
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
