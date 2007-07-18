//
// $Id$

package com.threerings.msoy.peer.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObserverList;
import com.samskivert.util.ResultListener;

import com.threerings.presents.server.PresentsClient;
import com.threerings.crowd.peer.server.CrowdPeerManager;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerNode;
import com.threerings.presents.peer.server.persist.NodeRecord;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.peer.data.HostedScene;
import com.threerings.msoy.peer.data.MsoyClientInfo;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import static com.threerings.msoy.Log.log;

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

    public MsoyPeerManager (ConnectionProvider conprov, Invoker invoker)
        throws PersistenceException
    {
        super(conprov, invoker);
    }

    /**
     * Registers an observer to be notified when remote player log on and off.
     */
    public void addRemoteMemberObserver (RemoteMemberObserver obs)
    {
        _remobs.add(obs);
    }

    /**
     * Clears out a remote member oberver registration.
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
        _mnobj.addToHostedScenes(new HostedScene(sceneId, name));
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
     * Returns the node name of the peer that is hosting the specified scene, or null if no peer
     * has published that they are hosting the scene.
     */
    public String getSceneHost (final int sceneId)
    {
        return lookupNodeDatum(new Lookup<String>() {
            public String lookup (NodeObject nodeobj) {
                HostedScene info = ((MsoyNodeObject)nodeobj).hostedScenes.get(sceneId);
                return (info == null) ? null : nodeobj.nodeName;
            }
        });
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

    @Override // from PeerManager
    protected PeerNode createPeerNode (NodeRecord record)
    {
        return new MsoyPeerNode(this, record);
    }

    /** A casted reference to our node object. */
    protected MsoyNodeObject _mnobj;

    /** Our remote member observers. */
    protected ObserverList<RemoteMemberObserver> _remobs =
        new ObserverList<RemoteMemberObserver>(ObserverList.FAST_UNSAFE_NOTIFY);
}
