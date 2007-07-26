//
// $Id$

package com.threerings.msoy.peer.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObserverList;
import com.samskivert.util.ResultListener;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.server.PresentsClient;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerNode;
import com.threerings.presents.peer.server.persist.NodeRecord;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.peer.server.CrowdPeerManager;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.world.server.RoomManager;

import com.threerings.msoy.peer.data.HostedPlace;
import com.threerings.msoy.peer.data.MemberLocation;
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

    public MsoyPeerManager (PersistenceContext perCtx, Invoker invoker)
        throws PersistenceException
    {
        super(perCtx, invoker);
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
            // skip "location=-1" updates as the client will either logoff or arrive at their real
            // location immediately following and that will trigger the real update
            if (event.getName().equals(MemberObject.LOCATION) &&
                event.getValue() != null) {
                MemberObject memobj = (MemberObject)MsoyServer.omgr.getObject(event.getTargetOid());
                if (memobj == null) {
                    log.warning("Got location change for unregistered member!? " + event);
                    return;
                }

                MemberLocation newloc = new MemberLocation();
                newloc.memberId = memobj.getMemberId();

                PlaceManager pmgr = MsoyServer.plreg.getPlaceManager(memobj.getPlaceOid());
                if (pmgr instanceof GameManager) {
                    newloc.type = MemberLocation.GAME;
                    newloc.locationId = ((MsoyGameConfig)pmgr.getConfig()).getGameId();
                } else if (pmgr instanceof RoomManager) {
                    newloc.type = MemberLocation.SCENE;
                    newloc.locationId = ((RoomManager)pmgr).getScene().getId();
                }

                if (_mnobj.memberLocs.contains(newloc)) {
                    _mnobj.updateMemberLocs(newloc);
                } else {
                    _mnobj.addToMemberLocs(newloc);
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
