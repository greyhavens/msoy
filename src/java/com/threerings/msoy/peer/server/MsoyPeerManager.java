//
// $Id$

package com.threerings.msoy.peer.server;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.ObserverList;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.io.Streamable;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.util.ConfirmAdapter;

import com.threerings.presents.peer.client.PeerService;
import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerNode;

import com.threerings.crowd.peer.server.CrowdPeerManager;

import com.threerings.whirled.data.ScenePlace;
import com.threerings.whirled.server.SceneRegistry;

import com.threerings.msoy.data.AuthName;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthName;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.party.data.MemberParty;
import com.threerings.msoy.party.data.PartySummary;
import com.threerings.msoy.room.server.MsoySceneRegistry;

import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MemberGame;
import com.threerings.msoy.peer.data.MemberScene;
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
        void memberEnteredScene (String peerName, int memberId, int sceneId);
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

    /**
     * Used to hear when peers connect or disconnect from this node.
     */
    public static interface PeerObserver
    {
        /** Called when a peer logs onto this node. */
        void connectedToPeer (MsoyNodeObject nodeobj);

        /** Called when a peer logs off of this node. */
        void disconnectedFromPeer (String node);
    }

    /** Useful with {@link #applyToNodes}. */
    public static abstract class NodeOp implements Function<NodeObject, Void>
    {
        public abstract void apply (MsoyNodeObject mnobj);

        public Void apply (NodeObject nodeobj) {
            apply((MsoyNodeObject)nodeobj);
            return null;
        }
    }

    /** Useful with {@link #lookupNodeDatum}. */
    public static abstract class NodeFunc<T> implements Function<NodeObject, T>
    {
        public abstract T apply (MsoyNodeObject mnobj);

        public T apply (NodeObject nodeobj) {
            return apply((MsoyNodeObject)nodeobj);
        }
    }

    /** Our {@link MemberObserver}s. */
    public final ObserverList<MemberObserver> memberObs = ObserverList.newFastUnsafe();

    /** Our {@link MemberForwardObserver}s. */
    public final ObserverList<MemberForwardObserver> memberFwdObs =
        ObserverList.newFastUnsafe();

    /** Our {@link PeerObserver}s. */
    public final ObserverList<PeerObserver> peerObs = ObserverList.newFastUnsafe();

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

    /**
     * Creates an uninitialized peer manager.
     */
    @Inject public MsoyPeerManager (ShutdownManager shutmgr)
    {
        super(shutmgr);
    }

    /**
     * Returns the scene occupied by the member, or 0 if they are not in a scene on any peer.
     */
    public int getMemberScene (int memberId)
    {
        final Integer memberKey = memberId;
        Integer sceneId = lookupNodeDatum(new NodeFunc<Integer>() {
            public Integer apply (MsoyNodeObject nodeobj) {
                MemberScene datum = nodeobj.memberScenes.get(memberKey);
                return (datum == null) ? null : datum.sceneId;
            }
        });
        return (sceneId == null) ? 0 : sceneId;
    }

    /**
     * Returns true if the supplied member is online anywhere in the network.
     */
    public boolean isMemberOnline (int memberId)
    {
        final Integer memberKey = memberId;
        Boolean online = lookupNodeDatum(new NodeFunc<Boolean>() {
            public Boolean apply (MsoyNodeObject mnobj) {
                if (mnobj.memberScenes.containsKey(memberKey)) {
                    return true;
                }
                if (mnobj.memberGames.containsKey(memberKey)) {
                    return true;
                }
                return null;
            }
        });
        return (online == null) ? false : online;
    }

    /**
     * Returns a set containing the ids of all members in the supplied set that are online.
     */
    public Set<Integer> filterOnline (final Set<Integer> memberIds)
    {
        final Set<Integer> onlineIds = Sets.newHashSet();
        applyToNodes(new NodeOp() {
            public void apply (MsoyNodeObject mnobj) {
                for (MemberScene scene : mnobj.memberScenes) {
                    if (memberIds.contains(scene.memberId)) {
                        onlineIds.add(scene.memberId);
                    }
                }
                for (MemberGame game : mnobj.memberGames) {
                    if (memberIds.contains(game.memberId)) {
                        onlineIds.add(game.memberId);
                    }
                }
            }
        });
        return onlineIds;
    }

    /**
     * Returns MemberLocation instances for all members in the supplied set.
     */
    public MemberLocation getMemberLocation (int memberId)
    {
        final Integer memberKey = memberId;
        final MemberLocation loc = new MemberLocation();
        applyToNodes(new NodeOp() {
            public void apply (MsoyNodeObject mnobj) {
                MemberScene scene = mnobj.memberScenes.get(memberKey);
                if (scene != null) {
                    loc.memberId = scene.memberId;
                    loc.sceneId = scene.sceneId;
                }
                MemberGame game = mnobj.memberGames.get(memberKey);
                if (game != null) {
                    loc.memberId = game.memberId;
                    loc.gameId = game.gameId;
                    loc.avrGame = game.avrGame;
                }
            }
        });
        return (loc.memberId != 0) ? loc : null;
    }

    /**
     * Get the PartySummary for the specified player, or null if they're not partying.
     */
    public PartySummary getPartySummary (int memberId)
    {
        final Integer memberKey = memberId;
        return lookupNodeDatum(new NodeFunc<PartySummary>() {
            public PartySummary apply (MsoyNodeObject nodeObj) {
                MemberParty mp = nodeObj.memberParties.get(memberKey);
                if (mp == null) {
                    return null;
                }
                return nodeObj.hostedParties.get(mp.partyId);
            }
        });
    }

    /**
     * Updates the scene occupied by the specified member on this server.
     *
     * @return true if the member's status changed, false if not.
     */
    public boolean updateMemberScene (MemberObject memobj)
    {
        if (_nodeobj.clients.get(memobj.username) == null) {
            return false; // they're leaving or left, so no need to worry
        }

        Integer memberId = memobj.getMemberId();
        int sceneId = Math.max(memobj.getSceneId(), 0); // we use 0 for no scene
        MemberScene datum = _mnobj.memberScenes.get(memberId);
        if (datum == null) {
            if (sceneId != 0) {
                _mnobj.addToMemberScenes(new MemberScene(memberId, sceneId));
            } else {
                return false;
            }
        } else if (sceneId == 0) {
            _mnobj.removeFromMemberScenes(memberId);
        } else if (datum.sceneId != sceneId) {
            _mnobj.updateMemberScenes(new MemberScene(memberId, sceneId));
        } else {
            return false;
        }

        // notify our member observers
        memberEnteredScene(_nodeName, memberId, sceneId);
        return true;
    }

    /**
     * Updates the game (or game lobby) occupied by the specified member on this server.
     *
     * @return true if the member's status changed, false if not.
     */
    public boolean updateMemberGame (int memberId, GameSummary game)
    {
        if (_nodeobj.clients.get(GameAuthName.makeKey(memberId)) == null) {
            return false; // they're leaving or left, so no need to worry
        }

        Integer memberKey = memberId;
        MemberGame datum = _mnobj.memberGames.get(memberKey);
        if (datum == null) {
            if (game != null) {
                _mnobj.addToMemberGames(new MemberGame(memberKey, game.gameId, game.avrGame));
            } else {
                return false;
            }
        } else if (game == null) {
            _mnobj.removeFromMemberGames(memberKey);
        } else if (datum.gameId != game.gameId) {
            _mnobj.updateMemberGames(new MemberGame(memberKey, game.gameId, game.avrGame));
        } else {
            return false;
        }
        return true;
    }

    /**
     * Returns the node name of the peer that is hosting the specified scene, or null if no peer
     * has published that they are hosting the scene.
     */
    public Tuple<String, HostedRoom> getSceneHost (final int sceneId)
    {
        return lookupNodeDatum(new NodeFunc<Tuple<String, HostedRoom>>() {
            public Tuple<String, HostedRoom> apply (MsoyNodeObject nodeobj) {
                HostedRoom info = nodeobj.hostedScenes.get(sceneId);
                return (info == null) ? null : Tuple.newTuple(nodeobj.nodeName, info);
            }
        });
    }

    /**
     * Returns the name and game server port of the peer that is hosting the specified game, or
     * null if no peer has published that they are hosting the game.
     */
    public Tuple<String, HostedGame> getGameHost (final int gameId)
    {
        return lookupNodeDatum(new NodeFunc<Tuple<String, HostedGame>>() {
            public Tuple<String, HostedGame> apply (MsoyNodeObject nodeobj) {
                HostedGame info = nodeobj.hostedGames.get(gameId);
                return (info == null) ? null : Tuple.newTuple(nodeobj.nodeName, info);
            }
        });
    }

    /**
     * Called by the RoomManager when it is hosting a scene.
     */
    public void roomDidStartup (
        int sceneId, String name, int ownerId, byte ownerType, byte accessControl)
    {
        log.debug("Hosting scene", "id", sceneId, "name", name);
        _mnobj.addToHostedScenes(new HostedRoom(sceneId, name, ownerId, ownerType, accessControl));
    }

    /**
     * Called by the RoomManager when it is no longer hosting a scene.
     */
    public void roomDidShutdown (int sceneId)
    {
        log.debug("No longer hosting scene", "id", sceneId);
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
    public void gameDidStartup (int gameId, String name)
    {
        log.debug("Hosting game", "id", gameId, "name", name);
        _mnobj.addToHostedGames(new HostedGame(gameId, name));
        // releases our lock on this game now that it is resolved and we are hosting it
        releaseLock(getGameLock(gameId), new ResultListener.NOOP<String>());
    }

    /**
     * Called by the WorldGameRegistry when a game server has been shutdown.
     */
    public void gameDidShutdown (int gameId)
    {
        log.debug("No longer hosting game", "id", gameId);
        _mnobj.removeFromHostedGames(gameId);
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
     * Returns the internal hostname for to use that run on our own backend network, or null
     * if the peer is not currently connected to this server. Do not export this data to the
     * outside world in any shape or form.
     */
    public String getPeerInternalHostName (String nodeName)
    {
        if (_nodeName.equals(nodeName)) {
            return _self.hostName;
        }
        MsoyPeerNode peer = (MsoyPeerNode)_peers.get(nodeName);
        return (peer == null) ? null : peer.getInternalHostName();
    }

    /**
     * Returns the next party id that may be assigned by this server.
     * Only called from the PartyRegistry, does not need synchronization.
     */
    public int getNextPartyId ()
    {
        if (_partyIdCounter >= Integer.MAX_VALUE / MAX_NODES) {
            log.warning("ZOMG! We plumb run out of id space", "partyId", _partyIdCounter);
            _partyIdCounter = 0;
        }
        return (ServerConfig.nodeId + MAX_NODES * ++_partyIdCounter);
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
            if (entry == null) {
                _lastRequestName = username;
                _lastRequestTime = now;
            } else {
                _lastRequestName = null;
            }
            if ((entry == null) && (username instanceof AuthName) &&
                getMemberScene(((AuthName) username).getMemberId()) != 0) {
                log.warning("Asked for forwarded member object, on another node", "name", username);
            }
            return (entry != null && now < entry.expireTime) ?
                Tuple.newTuple(entry.memobj, entry.locals) : null;

        } finally {
            // clear other expired records from the cache
            for (Iterator<MemObjCacheEntry> itr = _mobjCache.values().iterator(); itr.hasNext();) {
                MemObjCacheEntry entry = itr.next();
                if (now > entry.expireTime) {
                    itr.remove();
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
            log.warning("Unable to forward member object to unready peer", "peer", nodeName,
                        "connected", (node != null), "member", memobj.memberName);
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

        // TEMP
        long dt = System.currentTimeMillis() - _lastRequestTime;
        if (dt < 60000 && memobj.username.equals(_lastRequestName)) {
            log.warning("Oh my lanta! We seem to be a little late for this stashing!",
                "user", memobj.username, "miss milliseconds", dt, new Exception());
        }
    }

    // TEMP
    protected Name _lastRequestName;
    protected long _lastRequestTime;

    /**
     * Requests that we forward the reclaimItem request to the appropriate server.
     */
    public void reclaimItem (
        String nodeName, int sceneId, int memberId, ItemIdent item,
        final ResultListener<Void> listener)
    {
        // locate the peer in question
        PeerNode node = _peers.get(nodeName);
        if (node == null || node.nodeobj == null) {
            // this should never happen.
            log.warning("Unable to reclaim item on unready peer",
               "peer", nodeName, "connected", (node != null), "item", item);
            listener.requestFailed(new Exception());
            return;
        }
        ((MsoyNodeObject)node.nodeobj).msoyPeerService.reclaimItem(
            node.getClient(), sceneId, memberId, item, new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                    listener.requestCompleted(null);
                }
                public void requestFailed (String cause) {
                    listener.requestFailed(new InvocationException(cause));
                }
            });
    }

    /**
     * Requests to forward a room ownership change to the appropriate peer.
     */
    public void transferRoomOwnership (
        String nodeName, int sceneId, byte ownerType, int ownerId, Name ownerName,
        boolean lockToOwner, final ResultListener<Void> listener)
    {
        // locate the peer in question
        PeerNode node = _peers.get(nodeName);
        if (node == null || node.nodeobj == null) {
            // this should never happen.
            log.warning("Unable to transfer room ownership on unready peer",
               "peer", nodeName, "connected", (node != null), "sceneId", sceneId);
            listener.requestFailed(new Exception());
            return;
        }
        ((MsoyNodeObject)node.nodeobj).msoyPeerService.transferRoomOwnership(
            node.getClient(), sceneId, ownerType, ownerId, ownerName, lockToOwner,
            new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                    listener.requestCompleted(null);
                }
                public void requestFailed (String cause) {
                    listener.requestFailed(new InvocationException(cause));
                }
            });
    }

    // from interface MsoyPeerProvider
    public void reclaimItem (
        ClientObject caller, int sceneId, int memberId, ItemIdent item,
        InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        ((MsoySceneRegistry) _screg).reclaimItem(sceneId, memberId, item,
            new ConfirmAdapter(listener));
    }

    // from interface MsoyPeerProvider
    public void transferRoomOwnership (
        ClientObject caller, int sceneId, byte ownerType, int ownerId, Name ownerName,
        boolean lockToOwner, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        ((MsoySceneRegistry) _screg).transferOwnership(sceneId, ownerType, ownerId, ownerName,
            lockToOwner, new ConfirmAdapter(listener));
    }

    @Override // from PeerManager
    public void generateReport (ClientObject caller, String type,
                                final PeerService.ResultListener listener)
        throws InvocationException
    {
        listener.requestProcessed(_reportMan.generateReport(type));
    }

    @Override // from PeerManager
    protected void didInit ()
    {
        super.didInit();

        // register our custom invocation service
        _mnobj.setMsoyPeerService(_invmgr.registerDispatcher(new MsoyPeerDispatcher(this)));
    }

    @Override // from PeerManager
    protected void clientLoggedOn (String nodeName, ClientInfo clinfo)
    {
        super.clientLoggedOn(nodeName, clinfo);

        if (clinfo.username instanceof MsoyAuthName) {
            memberLoggedOn(nodeName, (MsoyClientInfo)clinfo);
        }
    }

    @Override // from PeerManager
    protected void clientLoggedOff (String nodeName, ClientInfo clinfo)
    {
        super.clientLoggedOff(nodeName, clinfo);

        if (clinfo.username instanceof MsoyAuthName) {
            memberLoggedOff(nodeName, (MsoyClientInfo)clinfo);
        }
    }

    /**
     * Called when a member logs onto this or any other peer.
     */
    protected void memberLoggedOn (final String nodeName, final MsoyClientInfo info)
    {
        memberObs.apply(new ObserverList.ObserverOp<MemberObserver>() {
            public boolean apply (MemberObserver observer) {
                observer.memberLoggedOn(nodeName, (MemberName)info.visibleName);
                return true;
            }
        });
    }

    /**
     * Called when a member logs off of this or any other peer.
     */
    protected void memberLoggedOff (final String nodeName, final MsoyClientInfo info)
    {
        memberObs.apply(new ObserverList.ObserverOp<MemberObserver>() {
            public boolean apply (MemberObserver observer) {
                observer.memberLoggedOff(nodeName, (MemberName)info.visibleName);
                return true;
            }
        });
    }

    /**
     * Called when a member enters a new scene. Notifies observers.
     */
    protected void memberEnteredScene (final String node, final int memberId, final int sceneId)
    {
        memberObs.apply(new ObserverList.ObserverOp<MemberObserver>() {
            public boolean apply (MemberObserver observer) {
                observer.memberEnteredScene(node, memberId, sceneId);
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
    protected void initClientInfo (PresentsSession client, ClientInfo info)
    {
        super.initClientInfo(client, info);

        // if this is the primary session, add a location tracker
        if (info.username instanceof MsoyAuthName) {
            // we need never remove this as it should live for the duration of the session
            client.getClientObject().addListener(new LocationTracker());

            // let observers know that a member logged onto this node
            memberLoggedOn(_nodeName, (MsoyClientInfo)info);
        }
    }

    @Override // from PeerManager
    protected void clearClientInfo (PresentsSession client, ClientInfo info)
    {
        super.clearClientInfo(client, info);

        if (info.username instanceof MsoyAuthName) {
            // clear out their scene/game info in our node object
            Integer memberId = ((MsoyClientInfo)info).getMemberId();
            if (_mnobj.memberScenes.containsKey(memberId)) {
                _mnobj.removeFromMemberScenes(memberId);
                // TODO: memberEnteredScene(memberId, 0)?
            }
            if (_mnobj.memberGames.containsKey(memberId)) {
                _mnobj.removeFromMemberGames(memberId);
            }

            // notify observers that a member logged off of this node
            memberLoggedOff(_nodeName, (MsoyClientInfo)info);
        }
    }

    @Override // from PeerManager
    protected Class<? extends PeerNode> getPeerNodeClass ()
    {
        return MsoyPeerNode.class;
    }

    @Override // from PeerManager
    protected boolean ignoreClient (PresentsSession client)
    {
        // we only publish information about certain types of sessions
        return super.ignoreClient(client) || !isPublishedName(client.getUsername());
    }

    @Override // from PeerManager
    protected void connectedToPeer (PeerNode peer)
    {
        super.connectedToPeer(peer);

        // if we're on the dev server, maybe update our in-VM policy server
        if (DeploymentConfig.devDeployment && ServerConfig.socketPolicyPort > 1024 &&
                ServerConfig.nodeId == 1) {
            _msoyServer.addPortsToPolicy(ServerConfig.getServerPorts(peer.nodeobj.nodeName));
        }

        // notify our peer observers
        final MsoyNodeObject nodeobj = (MsoyNodeObject)peer.nodeobj;
        peerObs.apply(new ObserverList.ObserverOp<PeerObserver>() {
            public boolean apply (PeerObserver observer) {
                observer.connectedToPeer(nodeobj);
                return true;
            }
        });
    }

    @Override // from PeerManager
    protected void disconnectedFromPeer (PeerNode peer)
    {
        super.disconnectedFromPeer(peer);

        // if we're on the dev server, remove this server from our in-VM policy server
        if (DeploymentConfig.devDeployment && ServerConfig.socketPolicyPort > 1024 &&
                ServerConfig.nodeId == 1) {
            _msoyServer.removePortsFromPolicy(ServerConfig.getServerPorts(peer.nodeobj.nodeName));
        }

        // notify our peer observers
        final String nodeName = peer.nodeobj.nodeName;
        peerObs.apply(new ObserverList.ObserverOp<PeerObserver>() {
            public boolean apply (PeerObserver observer) {
                observer.disconnectedFromPeer(nodeName);
                return true;
            }
        });
    }

    /**
     * Returns true if the supplied authentication username is a type that we publish in our
     * NodeObject. Currently we do this for world and game sessions (but not for party sessions).
     */
    protected static boolean isPublishedName (Name authname)
    {
        return (authname instanceof MsoyAuthName) || (authname instanceof GameAuthName);
    }

    /** Used to keep {@link MsoyNodeObject#memberScenes} up to date. */
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
                    updateMemberScene(memobj);
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
    @Inject protected ClientManager _clmgr;
    @Inject protected InvocationManager _invmgr;
    @Inject protected ReportManager _reportMan;
    @Inject protected MsoyServer _msoyServer;
    @Inject protected SceneRegistry _screg;

    /** A counter used to assign party ids on this server. */
    protected static int _partyIdCounter;

    /** An arbitrary limit on the number of nodes allowed in our network so that we can partition
     * id space for guest member id assignment. It's a power of 10 to make id values look nicer. */
    protected static final int MAX_NODES = 1000;
}
