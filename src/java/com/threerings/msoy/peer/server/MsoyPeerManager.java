//
// $Id$

package com.threerings.msoy.peer.server;

import static com.threerings.msoy.Log.log;

import com.samskivert.util.ObserverList;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;
import com.threerings.crowd.peer.server.CrowdPeerManager;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.HostedPlace;
import com.threerings.msoy.peer.data.HostedProject;
import com.threerings.msoy.peer.data.MemberLocation;
import com.threerings.msoy.peer.data.MsoyClientInfo;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerNode;
import com.threerings.presents.peer.server.persist.NodeRecord;
import com.threerings.presents.server.PresentsClient;
import com.threerings.whirled.data.ScenePlace;

/**
 * Manages communication with our peer servers, coordinates services that must work across peers.
 */
public class MsoyPeerManager extends CrowdPeerManager
{
    /** Used to notify interested parties when members log onto and off of remote servers. */
    public static interface RemoteMemberObserver
    {
        /** Called when this member has logged onto another server. */
        public void remoteMemberLoggedOn (MemberName member);

        /** Called when this member has logged off of another server. */
        public void remoteMemberLoggedOff (MemberName member);
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
        MemberLocation newloc = new MemberLocation();
        newloc.memberId = memobj.getMemberId();
        newloc.sceneId = Math.max(ScenePlace.getSceneId(memobj), 0); // we use 0 for no scene
        newloc.gameId = (memobj.game == null) ? 0 : memobj.game.gameId;

        if (_mnobj.memberLocs.contains(newloc)) {
            _mnobj.updateMemberLocs(newloc);
        } else {
            _mnobj.addToMemberLocs(newloc);
        }
    }

    /**
     * Returns the node name of the peer that is hosting the specified scene, or null if no peer
     * has published that they are hosting the scene.
     */
    public String getSceneHost (final int sceneId)
    {
        return lookupNodeDatum(new Lookup<String>() {
            public String lookup (NodeObject nodeobj) {
                HostedPlace info = ((MsoyNodeObject)nodeobj).hostedScenes.get(sceneId);
                return (info == null) ? null : nodeobj.nodeName;
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
    public HostedProject getProjectHost (final int projectId)
    {
        return lookupNodeDatum(new Lookup<HostedProject>() {
            public HostedProject lookup (NodeObject nodeobj) {
                HostedProject info = ((MsoyNodeObject) nodeobj).hostedProjects.get(projectId);
                return info;
            }
        });
    }

    /**
     * Returns the node of the peer that is hosting the specified chat channel, or null if no peer
     * has published that they are hosting the channel.
     */
    public MsoyNodeObject getChannelHost (final ChatChannel channel)
    {
        final Comparable channelKey = HostedChannel.getKey(channel);
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
    public void roomDidStartup (int sceneId, String name)
    {
        log.info("Hosting scene [id=" + sceneId + ", name=" + name + "].");
        _mnobj.addToHostedScenes(new HostedPlace(sceneId, name));
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
        _mnobj.addToHostedProjects(new HostedProject(project, config, getNodeObject().nodeName));
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
    }

    @Override // from PeerManager
    protected void clearClientInfo (PresentsClient client, ClientInfo info)
    {
        super.clearClientInfo(client, info);

        // clear out their location in our node object (if they were in one)
        Integer memberId = ((MsoyClientInfo)info).getMemberId();
        if (_mnobj.memberLocs.containsKey(memberId)) {
            _mnobj.removeFromMemberLocs(memberId);
        }
    }

    @Override // from PeerManager
    protected PeerNode createPeerNode (NodeRecord record)
    {
        return new MsoyPeerNode(this, record);
    }

    /** Used to keep {@link MsoyNodeObject#memberLocs} up to date. */
    protected class LocationTracker implements AttributeChangeListener
    {
        public void attributeChanged (AttributeChangedEvent event) {
            // skip null location updates unless we have a game attached to this MemberObject.  In
            // that case, the null location could mean heading to the game, and we do need to zero
            // out the sceneId on this player's MemberLocation.
            if (event.getName().equals(MemberObject.LOCATION)) {
                MemberObject memobj = (MemberObject)MsoyServer.omgr.getObject(event.getTargetOid());
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

    /** A casted reference to our node object. */
    protected MsoyNodeObject _mnobj;

    /** Our remote member observers. */
    protected ObserverList<RemoteMemberObserver> _remobs =
        new ObserverList<RemoteMemberObserver>(ObserverList.FAST_UNSAFE_NOTIFY);
}
