//
// $Id$

package com.threerings.msoy.peer.server;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.samskivert.util.ObserverList;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.io.Streamable;
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

import com.threerings.msoy.web.gwt.ConnectConfig;

import com.threerings.msoy.bureau.server.BureauLauncherServerClient;
import com.threerings.msoy.bureau.server.WindowServerClient;
import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MsoyClient;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;

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
    /**
     * Used to notify interested parties when members move between scenes and when they log onto and
     * off of servers. This includes peer servers and this local server, therefore all member
     * movement may be monitored via this interface.
     */
    public static interface MemberObserver
    {
        /**
         * Notifies the observer when a member has logged onto an msoy server.
         */
        void memberLoggedOn (String node, MemberName member);

        /**
         * Notifies the observer when a member has logged off of an msoy server.
         */
        void memberLoggedOff (String peerName, MemberName member);

        /**
         * Notifies the observer when a member has entered a new scene.
         */
        void memberEnteredScene (String peerName, MemberLocation loc);
    }

    /**
     * Used to hear about members being forwarded between servers.
     */
    public static interface MemberForwardObserver
    {
        /**
         * Called when the supplied member object is about to be sent to the specified node.
         */
        void memberWillBeSent (String node, MemberObject member);
    }

    /** Our {@link MemberObserver}s. */
    public final ObserverList<MemberObserver> memberObs = ObserverList.newFastUnsafe();

    /** Our {@link MemberForwardObserver}s. */
    public final ObserverList<MemberForwardObserver> memberFwdObs =
        ObserverList.newFastUnsafe();

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

    /**
     * Creates an uninitialized peer manager.
     */
    @Inject public MsoyPeerManager (ShutdownManager shutmgr)
    {
        super(shutmgr);
    }

    /**
     * Returns the location of the specified member, or null if they are not online on any peer.
     */
    public MemberLocation getMemberLocation (final int memberId)
    {
        return lookupNodeDatum(new Function<NodeObject,MemberLocation>() {
            public MemberLocation apply (NodeObject nodeobj) {
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

        memberEnteredScene(_nodeName, newloc);
    }

    /**
     * Returns the node name of the peer that is hosting the specified scene, or null if no peer
     * has published that they are hosting the scene.
     */
    public Tuple<String, HostedRoom> getSceneHost (final int sceneId)
    {
        return lookupNodeDatum(new Function<NodeObject,Tuple<String,HostedRoom>>() {
            public Tuple<String,HostedRoom> apply (NodeObject nodeobj) {
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
        return lookupNodeDatum(new Function<NodeObject,Tuple<String,Integer>>() {
            public Tuple<String,Integer> apply (NodeObject nodeobj) {
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
        return lookupNodeDatum(new Function<NodeObject,ConnectConfig>() {
            public ConnectConfig apply (NodeObject nodeobj) {
                HostedProject info = ((MsoyNodeObject) nodeobj).hostedProjects.get(projectId);
                return (info == null) ? null : info.createConnectConfig();
            }
        });
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
     * Called by the WorldGameRegistry when we have established a new game server to host a
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
     * Called by the WorldGameRegistry when a game server has been shutdown.
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
     * Returns member info forwarded from one of our peers if we have any, null otherwise.
     */
    public Tuple<MemberObject,Streamable[]> getForwardedMemberObject (Name username)
    {
        long now = System.currentTimeMillis();
        try {
            // locate our forwarded member object if any
            MemObjCacheEntry entry = _mobjCache.remove(username);
            return (entry != null && now < entry.expireTime) ?
                Tuple.create(entry.memobj, entry.locals) : null;

        } finally {
            // clear other expired records from the cache
            for (Iterator<Map.Entry<Name,MemObjCacheEntry>> iter =
                     _mobjCache.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<Name,MemObjCacheEntry> entry = iter.next();
                if (now < entry.getValue().expireTime) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * Requests that we forward the supplied member object to the specified peer.
     */
    public void forwardMemberObject (final String nodeName, final MemberObject memobj)
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

        // let our member observers know what's up
        memberFwdObs.apply(new ObserverList.ObserverOp<MemberForwardObserver>() {
            public boolean apply (MemberForwardObserver observer) {
                observer.memberWillBeSent(nodeName, memobj);
                return true;
            }
        });

        // forward any streamable local attributes
        List<Streamable> locals = Lists.newArrayList();
        for (Object local : memobj.getLocals()) {
            if (local instanceof Streamable) {
                locals.add((Streamable)local);
            }
        }

        // do the forwarding deed
        ((MsoyNodeObject)node.nodeobj).msoyPeerService.forwardMemberObject(
            node.getClient(), memobj, locals.toArray(new Streamable[locals.size()]));
    }

    // from interface MsoyPeerProvider
    public void forwardMemberObject (ClientObject caller, MemberObject memobj, Streamable[] locals)
    {
        // clear out various bits in the received object
        memobj.clearForwardedObject();

        // place this member object in a temporary cache; if the member in question logs on in the
        // next 30 seconds, we'll use this object instead of re-resolving all of their data
        _mobjCache.put(memobj.username, new MemObjCacheEntry(memobj, locals));
    }

    /**
     * Called when a member logs onto a server. Notifies observers.
     */
    protected void memberLoggedOn (final String node, final MsoyClientInfo info)
    {
        memberObs.apply(new ObserverList.ObserverOp<MemberObserver>() {
            public boolean apply (MemberObserver observer) {
                observer.memberLoggedOn(node, (MemberName)info.visibleName);
                return true;
            }
        });
    }

    /**
     * Called when a member logs off of a server. Notifies observers.
     */
    protected void memberLoggedOff (final String node, final MsoyClientInfo info)
    {
        memberObs.apply(new ObserverList.ObserverOp<MemberObserver>() {
            public boolean apply (MemberObserver observer) {
                observer.memberLoggedOff(node, (MemberName)info.visibleName);
                return true;
            }
        });
    }

    /**
     * Called when a member enters a new scene. Notifies observers.
     */
    protected void memberEnteredScene (final String node, final MemberLocation loc)
    {
        memberObs.apply(new ObserverList.ObserverOp<MemberObserver>() {
            public boolean apply (MemberObserver observer) {
                observer.memberEnteredScene(node, loc);
                return true;
            }
        });
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
        
        // notify observers
        memberLoggedOn(_nodeName, (MsoyClientInfo)info);
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
        
        // notify observers
        memberLoggedOff(_nodeName, (MsoyClientInfo)info);
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

    /** Used to keep {@link MsoyNodeObject#memberLocs} up to date. */
    protected class LocationTracker implements AttributeChangeListener
    {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(MemberObject.LOCATION)) {
                MemberObject memobj = (MemberObject)_omgr.getObject(event.getTargetOid());
                if (memobj == null) {
                    log.warning("Got location change for unregistered member!? " + event);
                    return;
                }

                // Ignore subsequent notifications if the member location has changed multiple 
                // times. This is alright because we do not actually use the event value anyway.
                if (event.getValue() != memobj.location) {
                    return;
                }                    

                // Skip null location updates unless we have a game attached to this MemberObject.
                // In that case, the null location could mean heading to the game, and we do need 
                // to zero out the sceneId on this player's MemberLocation.
                if (event.getValue() instanceof ScenePlace || memobj.game != null) {
                    updateMemberLocation(memobj);
                }
            }
        }
    }

    /** Used to cache forwarded member objects. */
    protected static class MemObjCacheEntry
    {
        public final long expireTime;
        public final MemberObject memobj;
        public final Streamable[] locals;

        public MemObjCacheEntry (MemberObject memobj, Streamable[] locals) {
            this.expireTime = System.currentTimeMillis() + 60*1000L;
            this.memobj = memobj;
            this.locals = locals;
        }
    }

    /** A casted reference to our node object. */
    protected MsoyNodeObject _mnobj;

    /** A cache of forwarded member objects. */
    protected Map<Name,MemObjCacheEntry> _mobjCache = Maps.newHashMap();

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
