//
// $Id$

package com.threerings.msoy.peer.server;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.samskivert.util.ObserverList;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerNode;

import com.threerings.crowd.peer.server.CrowdPeerManager;

import com.threerings.whirled.data.ScenePlace;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.web.data.ConnectConfig;

import com.threerings.msoy.bureau.server.BureauLauncherServerClient;
import com.threerings.msoy.bureau.server.WindowServerClient;
import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyClient;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;

import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.HostedProject;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MsoyClientInfo;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import static com.threerings.msoy.Log.log;

/**
 * Manages communication with our peer servers, coordinates services that must work across peers.
 */
@Singleton @EventThread
public class MsoyPeerManager extends CrowdPeerManager
    implements MsoyPeerProvider
{
    /** Used to notify interested parties when members log onto and off of remote servers. */
    public static interface RemoteMemberObserver
    {
        /** Called when this member has logged onto another server. */
        void remoteMemberLoggedOn (MemberName member);

        /** Called when this member has logged off of another server. */
        void remoteMemberLoggedOff (MemberName member);

        /** Called when this member has entered a new scene within a world server. */
        void remoteMemberEnteredScene (MemberLocation loc, String hostname, int port);
    }

    /** Used to participate in the member object forwarding process. */
    public static interface MemberForwarder
    {
        /**
         * Packs up additional data to be forwarded along with the member object. Anything added to
         * the supplied map will be sent to the other server. Note: all values in the map must be
         * streamable types.
         */
        void packMember (MemberObject memobj, Map<String,Object> data);

        /**
         * Unpacks additional data delivered with a forwarded member object.
         */
        void unpackMember (MemberObject memobj, Map<String,Object> data);
    }

    /** Returns a lock used to claim resolution of the specified scene. */
    public static NodeObject.Lock getSceneLock (int sceneId)
    {
        return new NodeObject.Lock("SceneHost", sceneId);
    }

    /** Returns a lock used to claim resolution of the specified game/game loboby. */
    public static NodeObject.Lock getGameLock (int gameId)
    {
        return new NodeObject.Lock("GameHost", gameId);
    }

    /** Returns a lock used to claim resolution of the specified Swiftly project room. */
    public static NodeObject.Lock getProjectLock (int projectId)
    {
        return new NodeObject.Lock("ProjectHost", projectId);
    }

    /** Returns a lock used to claim resolution of the specified chat channel. */
    public static NodeObject.Lock getChannelLock (ChatChannel channel)
    {
        return new NodeObject.Lock("ChannelHost", HostedChannel.getKey(channel));
    }

    /**
     * Creates an uninitialized peer manager.
     */
    @Inject public MsoyPeerManager (ShutdownManager shutmgr)
    {
        super(shutmgr);
    }

    /**
     * Registers a participant in the member forwarding process. This should be done during server
     * initialization, before we are likely to have to forward member objects. Note: there is no
     * way to remove a registration, the assumption is that all participants are registered at
     * server startup time and exist for the lifetime of the server.
     */
    public void registerMemberForwarder (MemberForwarder part)
    {
        _mforwarders.add(part);
    }

    /**
     * Returns the location of the specified member, or null if they are not online on any peer.
     */
    public MemberLocation getMemberLocation (final int memberId)
    {
        return lookupNodeDatum(new Lookup<MemberLocation>() {
            public MemberLocation lookup (NodeObject nodeobj) {
                return ((MsoyNodeObject)nodeobj).memberLocs.get(memberId);
            }
        });
    }

    /**
     * Reports the new location of a member on this server.
     */
    public void updateMemberLocation (MemberObject memobj)
    {
        MsoyClientInfo info = (MsoyClientInfo)_nodeobj.clients.get(memobj.memberName);
        if (info == null) {
            return; // they're leaving or left, so no need to worry
        }

        MemberLocation newloc = new MemberLocation();
        newloc.memberId = memobj.getMemberId();
        newloc.sceneId = Math.max(ScenePlace.getSceneId(memobj), 0); // we use 0 for no scene
        if (memobj.game != null) {
            newloc.gameId = memobj.game.gameId;
            newloc.avrGame = memobj.game.avrGame;
        } else {
            newloc.gameId = 0;
            newloc.avrGame = false;
        }

        if (_mnobj.memberLocs.contains(newloc)) {
            log.info("Updating member " + newloc + ".");
            _mnobj.updateMemberLocs(newloc);
        } else {
            log.info("Hosting member " + newloc + ".");
            _mnobj.addToMemberLocs(newloc);
        }

        memberEnteredScene(newloc, _self.publicHostName, _self.port);
    }

    /**
     * Returns the node name of the peer that is hosting the specified scene, or null if no peer
     * has published that they are hosting the scene.
     */
    public Tuple<String, HostedRoom> getSceneHost (final int sceneId)
    {
        return lookupNodeDatum(new Lookup<Tuple<String, HostedRoom>>() {
            public Tuple<String, HostedRoom> lookup (NodeObject nodeobj) {
                HostedRoom info = ((MsoyNodeObject)nodeobj).hostedScenes.get(sceneId);
                return (info == null) ? null :
                    new Tuple<String, HostedRoom>(nodeobj.nodeName, info);
            }
        });
    }

    /**
     * Returns the name and game server port of the peer that is hosting the specified game, or
     * null if no peer has published that they are hosting the game.
     */
    public Tuple<String, Integer> getGameHost (final int gameId)
    {
        return lookupNodeDatum(new Lookup<Tuple<String, Integer>>() {
            public Tuple<String, Integer> lookup (NodeObject nodeobj) {
                HostedGame info = ((MsoyNodeObject) nodeobj).hostedGames.get(gameId);
                return (info == null) ? null :
                    new Tuple<String, Integer>(nodeobj.nodeName, info.port);
            }
        });
    }

    /**
     * Returns the ConnectConfig for the Node hosting the Swiftly project room manager for this
     * project or null if no peer has published that they are hosting the project.
     */
    public ConnectConfig getProjectConnectConfig (final int projectId)
    {
        return lookupNodeDatum(new Lookup<ConnectConfig>() {
            public ConnectConfig lookup (NodeObject nodeobj) {
                HostedProject info = ((MsoyNodeObject) nodeobj).hostedProjects.get(projectId);
                return (info == null) ? null : info.createConnectConfig();
            }
        });
    }

    /**
     * Returns the node of the peer that is hosting the specified chat channel, or null if no peer
     * has published that they are hosting the channel.
     */
    public MsoyNodeObject getChannelHost (final ChatChannel channel)
    {
        final Comparable<?> channelKey = HostedChannel.getKey(channel);
        return lookupNodeDatum(new Lookup<MsoyNodeObject>() {
            public MsoyNodeObject lookup (NodeObject nodeobj) {
                MsoyNodeObject node = (MsoyNodeObject) nodeobj;
                return node.hostedChannels.get(channelKey) == null ? null : node;
            }
        });
    }

    /**
     * Registers an observer to be notified when remote player log on and off.
     */
    public void addRemoteMemberObserver (RemoteMemberObserver obs)
    {
        _remobs.add(obs);
    }

    /**
     * Clears out a remote member observer registration.
     */
    public void removeRemoteMemberObserver (RemoteMemberObserver obs)
    {
        _remobs.remove(obs);
    }

    /**
     * Called by the RoomManager when it is hosting a scene.
     */
    public void roomDidStartup (int sceneId, String name, int ownerId, byte ownerType,
        byte accessControl)
    {
        log.info("Hosting scene [id=" + sceneId + ", name=" + name + "].");
        _mnobj.addToHostedScenes(new HostedRoom(sceneId, name, ownerId, ownerType, accessControl));
        // release our lock on this scene now that it is resolved and we are hosting it
        releaseLock(getSceneLock(sceneId), new ResultListener.NOOP<String>());
    }

    /**
     * Called by the RoomManager when it is no longer hosting a scene.
     */
    public void roomDidShutdown (int sceneId)
    {
        log.info("No longer hosting scene [id=" + sceneId + "].");
        _mnobj.removeFromHostedScenes(sceneId);
    }

    /**
     * Called by the RoomManager when information pertinant to the HostedRoom has been updated.
     */
    public void roomUpdated (int sceneId, String name, int ownerId, byte ownerType,
        byte accessControl)
    {
        _mnobj.updateHostedScenes(new HostedRoom(sceneId, name, ownerId, ownerType, accessControl));
    }

    /**
     * Called by the MsoyGameRegistry when we have established a new game server to host a
     * particular game.
     */
    public void gameDidStartup (int gameId, String name, int port)
    {
        log.info("Hosting game [id=" + gameId + ", name=" + name + "].");
        _mnobj.addToHostedGames(new HostedGame(gameId, name, port));
        // releases our lock on this game now that it is resolved and we are hosting it
        releaseLock(getGameLock(gameId), new ResultListener.NOOP<String>());
    }

    /**
     * Called by the MsoyGameRegistry when a game server has been shutdown.
     */
    public void gameDidShutdown (int gameId)
    {
        log.info("No longer hosting game [id=" + gameId + "].");
        _mnobj.removeFromHostedGames(gameId);
    }

    /**
     * Called by the SwiftlyManager when it is hosting a project.
     */
    public void projectDidStartup (SwiftlyProject project, ConnectConfig config)
    {
        log.info(
            "Hosting project [id=" + project.projectId + ", name=" + project.projectName + "].");
        _mnobj.addToHostedProjects(new HostedProject(project, config));
    }

    /**
     * Called by the SwiftlyManager when it is no longer hosting a project.
     */
    public void projectDidShutdown (int projectId)
    {
        log.info("No longer hosting project [id=" + projectId + "].");
        _mnobj.removeFromHostedProjects(projectId);
    }

    /**
     * Returns the HTTP port this Whirled node is listening on.
     */
    public int getPeerHttpPort (String nodeName)
    {
        MsoyPeerNode peer = (MsoyPeerNode)_peers.get(nodeName);
        return (peer == null) ? -1 : peer.getHttpPort();
    }

    /**
     * Returns the next guest id that may be assigned by this server. Increments our internal guest
     * id counter in the process. This is called by the authenticator and is thus synchronized in
     * case we decide to switch to a pool of authenticator threads some day.
     */
    public synchronized int getNextGuestId ()
    {
        if (_guestIdCounter >= Integer.MAX_VALUE / MAX_NODES) {
            log.warning("ZOMG! We plumb run out of id space [id=" + _guestIdCounter + "].");
            _guestIdCounter = 0;
        }
        return -(ServerConfig.nodeId + MAX_NODES * ++_guestIdCounter);
    }

    /**
     * Returns a {@link MemberObject} forwarded from one of our peers if we have one. False if not.
     */
    public MemberObject getForwardedMemberObject (Name username)
    {
        long now = System.currentTimeMillis();
        try {
            // locate our forwarded member object if any
            MemObjCacheEntry entry = _mobjCache.remove(username);
            return (entry != null && now < entry.expireTime) ? entry.memobj : null;

        } finally {
            // clear other expired records from the cache
            if (_mobjCache.size() > 0) {
                for (Iterator<Map.Entry<Name,MemObjCacheEntry>> iter =
                         _mobjCache.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry<Name,MemObjCacheEntry> entry = iter.next();
                    if (now < entry.getValue().expireTime) {
                        iter.remove();
                    }
                }
            }
        }
    }

    /**
     * Requests that we forward the supplied member object to the specified peer.
     */
    public void forwardMemberObject (String nodeName, MemberObject memobj)
    {
        // we don't forward "featured place" clients' member objects because they contain no
        // meaningful information; guests and normal members do require forwarding
        if (memobj.getMemberId() == 0) {
            return;
        }

        // locate the peer in question
        PeerNode node = _peers.get(nodeName);
        if (node == null || node.nodeobj == null) {
            log.warning("Unable to forward member object to unready peer [peer=" + nodeName +
                        ", connected=" + (node != null) + ", member=" + memobj.memberName + "].");
            return;
        }

        // allow the member forward participants to participate
        Map<String,Object> data = Maps.newHashMap();
        for (MemberForwarder part : _mforwarders) {
            part.packMember(memobj, data);
        }

        // flatten the additional participant data into an array
        String[] keys = data.keySet().toArray(new String[data.size()]);
        Object[] values = data.values().toArray(new Object[data.size()]);

        // do the forwarding deed
        ((MsoyNodeObject)node.nodeobj).msoyPeerService.forwardMemberObject(
            node.getClient(), memobj, keys, values);

        // let our client handler know that the session is not over but rather is being forwarded
        // to another server
        MsoyClient mclient = (MsoyClient)_clmgr.getClient(memobj.username);
        if (mclient != null) {
            mclient.setSessionForwarded(true);
        }
    }

    // from interface MsoyPeerProvider
    public void forwardMemberObject (ClientObject caller, MemberObject memobj,
                                     String[] keys, Object[] values)
    {
        // clear out various bits in the received object
        memobj.clearForwardedObject();

        // let our forward participants in on the action
        Map<String,Object> data = Maps.newHashMap();
        for (int ii = 0; ii < keys.length; ii++) {
            data.put(keys[ii], values[ii]);
        }
        for (MemberForwarder part : _mforwarders) {
            part.unpackMember(memobj, data);
        }

        // place this member object in a temporary cache; if the member in question logs on in the
        // next 30 seconds, we'll use this object instead of re-resolving all of their data
        _mobjCache.put(memobj.username, new MemObjCacheEntry(memobj));
    }

    /**
     * Called by the {@link MsoyPeerNode} when a member logs onto their server.
     */
    protected void remoteMemberLoggedOn (MsoyPeerNode node, final MsoyClientInfo info)
    {
        _remobs.apply(new ObserverList.ObserverOp<RemoteMemberObserver>() {
            public boolean apply (RemoteMemberObserver observer) {
                observer.remoteMemberLoggedOn((MemberName)info.visibleName);
                return true;
            }
        });
    }

    /**
     * Called by the {@link MsoyPeerNode} when a member logs off of their server.
     */
    protected void remoteMemberLoggedOff (MsoyPeerNode node, final MsoyClientInfo info)
    {
        _remobs.apply(new ObserverList.ObserverOp<RemoteMemberObserver>() {
            public boolean apply (RemoteMemberObserver observer) {
                observer.remoteMemberLoggedOff((MemberName)info.visibleName);
                return true;
            }
        });
    }

    /**
     * Called by the {@link MsoyPeerNode} when a member changes scene on their server.
     */
    public void remoteMemberEnteredScene (final MsoyPeerNode node, final MemberLocation loc)
    {
        memberEnteredScene(loc, node.getPublicHostName(), node.getPort());
    }

    @Override // from CrowdPeerManager
    protected NodeObject createNodeObject ()
    {
        return (_mnobj = new MsoyNodeObject());
    }

    @Override // from CrowdPeerManager
    protected ClientInfo createClientInfo ()
    {
        return new MsoyClientInfo();
    }

    @Override // from CrowdPeerManager
    protected void initClientInfo (PresentsClient client, ClientInfo info)
    {
        super.initClientInfo(client, info);

        // this is called when the client starts their session, so we can add our location tracking
        // listener here; we need never remove it as it should live for the duration of the session
        client.getClientObject().addListener(new LocationTracker());

        // register our custom invocation service
        _mnobj.setMsoyPeerService(_invmgr.registerDispatcher(new MsoyPeerDispatcher(this)));
    }

    @Override // from PeerManager
    protected void clearClientInfo (PresentsClient client, ClientInfo info)
    {
        super.clearClientInfo(client, info);

        // clear out their location in our node object (if they were in one)
        Integer memberId = ((MsoyClientInfo)info).getMemberId();
        if (_mnobj.memberLocs.containsKey(memberId)) {
            log.info("Clearing member " + _mnobj.memberLocs.get(memberId) + ".");
            _mnobj.removeFromMemberLocs(memberId);
        }
    }

    @Override // from PeerManager
    protected PeerNode createPeerNode ()
    {
        return new MsoyPeerNode();
    }

    @Override // from PeerManager
    protected boolean ignoreClient (PresentsClient client)
    {
        // don't publish information about anonymous lurkers to our peers
        return super.ignoreClient(client) || (client.getUsername() instanceof LurkerName) ||
            (client instanceof BureauLauncherServerClient) ||
            (client instanceof WindowServerClient);
    }

    @Override // from PeerManager
    protected void peerDidLogon (PeerNode peer)
    {
        super.peerDidLogon(peer);

        // scan this peer for guests authenticated by a previous incarnation of this server and
        // adjust our next guest id to account for those assigned ids
        int maxGuestId = 0;
        for (ClientInfo info : peer.nodeobj.clients) {
            int memberId = ((MsoyClientInfo)info).getMemberId();
            if (memberId < 0) { // guest ids are negative
                int nodeId = (-memberId) % MAX_NODES;
                if (nodeId == ServerConfig.nodeId) {
                    maxGuestId = Math.max(maxGuestId, (-memberId) / MAX_NODES);
                }
            }
        }
        if (maxGuestId > 0) {
            log.info("Adjusting next guest id due to extant users [node=" + peer.nodeobj.nodeName +
                     ", maxGuestId=" + maxGuestId + "].");
            synchronized (this) {
                _guestIdCounter = Math.max(_guestIdCounter, maxGuestId);
            }
        }
        
        if (DeploymentConfig.devDeployment && ServerConfig.socketPolicyPort > 1024 && 
                ServerConfig.nodeId == 1) {
            _msoyServer.addPortsToPolicy(
                ServerConfig.getServerPorts(peer.nodeobj.nodeName));
        }
    }
    
    @Override // from PeerManager
    protected void peerDidLogoff (PeerNode peer)
    {
        super.peerDidLogoff(peer);
        
        if (DeploymentConfig.devDeployment && ServerConfig.socketPolicyPort > 1024 && 
                ServerConfig.nodeId == 1) {
            _msoyServer.removePortsFromPolicy(
                ServerConfig.getServerPorts(peer.nodeobj.nodeName));
        }
    }

    // internal method for notifying observers of a member scene change - this is called
    // both for genuinely remote movement and for movement on this server
    protected void memberEnteredScene (
        final MemberLocation loc, final String hostname, final int port)
    {
        _remobs.apply(new ObserverList.ObserverOp<RemoteMemberObserver>() {
            public boolean apply (RemoteMemberObserver observer) {
                observer.remoteMemberEnteredScene(loc, hostname, port);
                return true;
            }
        });
    }

    /** Used to keep {@link MsoyNodeObject#memberLocs} up to date. */
    protected class LocationTracker implements AttributeChangeListener
    {
        public void attributeChanged (AttributeChangedEvent event) {
            // skip null location updates unless we have a game attached to this MemberObject.  In
            // that case, the null location could mean heading to the game, and we do need to zero
            // out the sceneId on this player's MemberLocation.
            if (event.getName().equals(MemberObject.LOCATION)) {
                MemberObject memobj = (MemberObject)_omgr.getObject(event.getTargetOid());
                if (memobj == null) {
                    log.warning("Got location change for unregistered member!? " + event);
                    return;
                }
                if (event.getValue() instanceof ScenePlace || memobj.game != null) {
                    updateMemberLocation(memobj);
                }
            }
        }
    }

    /** Used to cache forwarded member objects. */
    protected static class MemObjCacheEntry
    {
        public long expireTime;
        public MemberObject memobj;

        public MemObjCacheEntry (MemberObject memobj) {
            expireTime = System.currentTimeMillis() + 60*1000L;
            this.memobj = memobj;
        }
    }

    /** A casted reference to our node object. */
    protected MsoyNodeObject _mnobj;

    /** Our remote member observers. */
    protected ObserverList<RemoteMemberObserver> _remobs = ObserverList.newFastUnsafe();

    /** A cache of forwarded member objects. */
    protected Map<Name,MemObjCacheEntry> _mobjCache = Maps.newHashMap();

    /** A list of participants in the member forwarding process. */
    protected List<MemberForwarder> _mforwarders = Lists.newArrayList();

    // dependencies
    @Inject protected InvocationManager _invmgr;
    @Inject protected ClientManager _clmgr;
    @Inject protected MsoyServer _msoyServer;

    /** A counter used to assign guest ids on this server. See {@link #getNextGuestId}. */
    protected static int _guestIdCounter;

    /** An arbitrary limit on the number of nodes allowed in our network so that we can partition
     * id space for guest member id assignment. It's a power of 10 to make id values look nicer. */
    protected static final int MAX_NODES = 1000;
}
