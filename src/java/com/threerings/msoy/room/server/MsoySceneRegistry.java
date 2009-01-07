//
// $Id$

package com.threerings.msoy.room.server;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.LocationManager;

import com.threerings.whirled.client.SceneMoveAdapter;
import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.data.SceneCodes;
import com.threerings.whirled.server.SceneManager;
import com.threerings.whirled.server.SceneMoveHandler;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.server.SpotSceneRegistry;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;

import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.party.server.PartyRegistry;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyPortal;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.PetObject;
import com.threerings.msoy.room.data.RoomCodes;

import static com.threerings.msoy.Log.log;

/**
 * Handles some custom Whirled scene traversal business.
 */
@Singleton
public class MsoySceneRegistry extends SpotSceneRegistry
    implements MsoySceneProvider
{
    /**
     * Extends ResolutionListener with peer-awareness.
     */
    public static interface PeerSceneResolutionListener extends ResolutionListener
    {
        /**
         * Called when the scene is already hosted on another node.
         */
        public void sceneOnNode (Tuple<String, HostedRoom> nodeInfo);
    }

    /**
     * A SceneMoveHandler that can receive the sceneOnNode() callback.
     */
    public static abstract class PeerSceneMoveHandler extends SceneMoveHandler
        implements PeerSceneResolutionListener
    {
        public PeerSceneMoveHandler (
            LocationManager locman, BodyObject body, int sceneVer,
            SceneService.SceneMoveListener listener)
        {
            super(locman, body, sceneVer, listener);
        }
    }

    @Inject public MsoySceneRegistry (InvocationManager invmgr)
    {
        super(invmgr);
        invmgr.registerDispatcher(new MsoySceneDispatcher(this), SceneCodes.WHIRLED_GROUP);
    }

//    @Override
//    public void resolveScene (int sceneId, ResolutionListener listener)
//    {
//        throw new RuntimeException("Use resolvePeerScene()");
//    }

    /**
     * Resolve a scene, or return the information on the peer on which it's hosted.
     */
    public void resolvePeerScene (final int sceneId, final PeerSceneResolutionListener listener)
    {
        // check to see if the destination scene is already hosted on a server
        Tuple<String, HostedRoom> nodeInfo = _peerMan.getSceneHost(sceneId);

        // if it's already hosted...
        if (nodeInfo != null) {
            // it's hosted on this server! It should already be resolved...
            if (_peerMan.getNodeObject().nodeName.equals(nodeInfo.left)) {
                super.resolveScene(sceneId, listener);
            } else {
                listener.sceneOnNode(nodeInfo); // somewhere else, pass the buck
            }
            return;
        }

        // otherwise the scene is not resolved here nor there; so we claim the scene by acquiring a
        // distributed lock and then resolve it locally
        _peerMan.acquireLock(MsoyPeerManager.getSceneLock(sceneId), new ResultListener<String>() {
            public void requestCompleted (String nodeName) {
                if (_peerMan.getNodeObject().nodeName.equals(nodeName)) {
                    log.debug("Got lock, resolving scene", "sceneId", sceneId);
                    MsoySceneRegistry.super.resolveScene(sceneId, new ResolutionListener() {
                        public void sceneWasResolved (SceneManager scmgr) {
                            listener.sceneWasResolved(scmgr);
                        }
                        public void sceneFailedToResolve (int sceneId, Exception reason) {
                            _peerMan.releaseSceneLock(sceneId);
                            listener.sceneFailedToResolve(sceneId, reason);
                        }
                    });

                } else {
                    // we didn't get the lock, so let's see what happened by re-checking
                    Tuple<String, HostedRoom> nodeInfo = _peerMan.getSceneHost(sceneId);
                    if (nodeName == null || nodeInfo == null || !nodeName.equals(nodeInfo.left)) {
                        log.warning("Scene resolved on wacked-out node?",
                            "sceneId", sceneId, "nodeName", nodeName, "nodeInfo", nodeInfo);
                        listener.sceneFailedToResolve(sceneId, new Exception("Wackedout"));
                    } else {
                        listener.sceneOnNode(nodeInfo); // somewhere else
                    }
                }
            }
            public void requestFailed (Exception cause) {
                log.warning("Failed to acquire scene resolution lock", "id", sceneId, cause);
                listener.sceneFailedToResolve(sceneId, cause);
            }
        });
    }

    /**
     * Called by the RoomManager when a member updates a room.
     */
    public void memberUpdatedRoom (MemberObject user, final MsoyScene scene)
    {
        int memId = user.getMemberId();

        // record this edit to the grindy log
        _eventLog.roomUpdated(memId, scene.getId(), user.getVisitorId());
    }

    public void reclaimItem (
        final int sceneId, final int memberId, final ItemIdent item,
        final ResultListener<Void> listener)
    {
        resolvePeerScene(sceneId, new PeerSceneResolutionListener() {
            public void sceneWasResolved (SceneManager scmgr) {
                ((RoomManager)scmgr).reclaimItem(item, memberId);
                listener.requestCompleted(null);
            }

            public void sceneOnNode (Tuple<String, HostedRoom> nodeInfo) {
                _peerMan.reclaimItem(nodeInfo.left, sceneId, memberId, item, listener);
            }

            public void sceneFailedToResolve (int sceneId, Exception reason) {
                listener.requestFailed(reason);
            }
        });
    }

    // TODO: the other version of moveTo() needs to also become peer-aware

    // from interface MsoySceneProvider
    public void moveTo (ClientObject caller, final int sceneId, int version, final int portalId,
                        final MsoyLocation destLoc, final SceneService.SceneMoveListener listener)
        throws InvocationException
    {
        final MsoyBodyObject mover = (MsoyBodyObject)caller;
        final MemberObject memobj = (mover instanceof MemberObject) ? (MemberObject)mover : null;

        // if they are departing a scene hosted by this server, move them to the exit; if we fail
        // later, they will have walked to the exit and then received an error message, alas
        RoomManager srcmgr = (RoomManager)getSceneManager(mover.getSceneId());
        if (srcmgr != null) {
            // give the source scene manager a chance to do access control
            Portal dest = ((MsoyScene)srcmgr.getScene()).getPortal(portalId);
            if (dest != null) {
                String errmsg = srcmgr.mayTraversePortal(mover, dest);
                if (errmsg != null) {
                    throw new InvocationException(errmsg);
                }
                srcmgr.willTraversePortal(mover, dest);
            }
        }

        // if this is a member with followers, tell them all to make the same scene move
        if (memobj != null) {
            // inform the party registry
            _partyReg.playerWillMove(memobj, sceneId);

            // iterate over a copy of the DSet, as we may modify it via the MemberNodeActions
            for (MemberName follower : Lists.newArrayList(memobj.followers)) {
                // this will notify the follower to change scenes and if the follower cannot be
                // found or if the follower is found and is found no longer to be following this
                // leader, dispatch a second action requesting that the follower be removed from
                // the leader's follower set; welcome to the twisty world of distributed systems
                MemberNodeActions.followTheLeader(
                    follower.getMemberId(), memobj.getMemberId(), sceneId);
            }
        }

        // this fellow will handle the nitty gritty of our scene switch
        resolvePeerScene(sceneId, new PeerSceneMoveHandler(_locman, mover, version, listener) {
            public void sceneOnNode (Tuple<String, HostedRoom> nodeInfo) {
                if (memobj == null) {
                    log.warning("Non-member requested move that requires server switch?",
                        "who", mover.who(), "info", nodeInfo);
                    listener.requestFailed(RoomCodes.E_INTERNAL_ERROR);
                    return;
                }

                // first check access control on the remote scene
                HostedRoom hr = nodeInfo.right;
                if (memobj.canEnterScene(hr.placeId, hr.ownerId, hr.ownerType, hr.accessControl)) {
                    log.debug("Going to remote node", "who", mover.who(),
                        "where", sceneId + "@" + nodeInfo.left);
                    sendClientToNode(nodeInfo.left, memobj, listener);
                } else {
                    listener.requestFailed(RoomCodes.E_ENTRANCE_DENIED);
                }
            }

            protected void effectSceneMove (SceneManager scmgr) throws InvocationException {
                // create a fake "from" portal that contains our destination location
                MsoyPortal from = new MsoyPortal();
                from.targetPortalId = (short)-1;
                from.dest = destLoc;

                // let the destination room manager know that we're coming in "from" that portal
                RoomManager destmgr = (RoomManager)scmgr;
                destmgr.mapEnteringBody(mover, from);

                try {
                    super.effectSceneMove(destmgr);
                } catch (InvocationException ie) {
                    // if anything goes haywire, clear out our entering status
                    destmgr.clearEnteringBody(mover);
                    throw ie;
                }

                // for members, check for following entities
                if (memobj != null) {
                    // deal with pets
                    PetObject petobj = _petMan.getPetObject(memobj.walkingId);
                    if (petobj != null) {
                        moveTo(petobj, sceneId, Integer.MAX_VALUE, portalId, destLoc,
                               new SceneMoveAdapter());
                    }
                }
            }
        });
    }

    protected void sendClientToNode (String nodeName, MemberObject memobj,
                                     SceneService.SceneMoveListener listener)
    {
        String hostname = _peerMan.getPeerPublicHostName(nodeName);
        int port = _peerMan.getPeerPort(nodeName);
        if (hostname == null || port == -1) {
            log.warning("Lost contact with peer during scene move [node=" + nodeName + "].");
            // freak out and let the user try again at which point we will hopefully have cleaned
            // up after this failed peer and will resolve the scene ourselves
            listener.requestFailed(RoomCodes.INTERNAL_ERROR);
            return;
        }

        // remove them from their current room to flush e.g. avatar memories to the memobj
        _locman.leaveOccupiedPlace(memobj);

        // tell the client about the node's hostname and port
        listener.moveRequiresServerSwitch(hostname, new int[] { port });

        // forward this client's member object to the node to which they will shortly connect
        _peerMan.forwardMemberObject(nodeName, memobj);
    }

    // our dependencies
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MemberLocator _locator;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected PetManager _petMan;
    @Inject protected PartyRegistry _partyReg;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected FeedRepository _feedRepo;
}
