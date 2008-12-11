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

import com.threerings.whirled.client.SceneMoveAdapter;
import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.data.SceneCodes;
import com.threerings.whirled.server.SceneManager;
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
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.room.data.MsoyLocation;
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
    @Inject public MsoySceneRegistry (InvocationManager invmgr)
    {
        super(invmgr);
        invmgr.registerDispatcher(new MsoySceneDispatcher(this), SceneCodes.WHIRLED_GROUP);
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

    /**
     * Called by the RoomManager when a member publishes a room.
     */
    public void memberPublishedRoom (MemberObject user, final MsoyScene scene)
    {
    }

    // from interface MsoySceneProvider
    public void moveTo (ClientObject caller, final int sceneId, int version, final int portalId,
                        final MsoyLocation destLoc, final SceneService.SceneMoveListener listener)
        throws InvocationException
    {
        MsoyBodyObject mover = (MsoyBodyObject)caller;

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
        final MemberObject memobj = (mover instanceof MemberObject) ? (MemberObject)mover : null;
        if (memobj != null) {
            for (MemberName follower : memobj.followers) {
                // this will notify the follower to change scenes and if the follower cannot be
                // found or if the follower is found and is found no longer to be following this
                // leader, dispatch a second action requesting that the follower be removed from
                // the leader's follower set; welcome to the twisty world of distributed systems
                MemberNodeActions.followTheLeader(
                    follower.getMemberId(), memobj.getMemberId(), sceneId);
            }
        }

        // this fellow will handle the nitty gritty of our scene switch
        final MsoySceneMoveHandler handler =
            new MsoySceneMoveHandler(_locman, _peerMan, mover, version, destLoc, listener) {
            protected void effectSceneMove (SceneManager scmgr) throws InvocationException {
                super.effectSceneMove(scmgr);
                // if we're a member and we have a pet following us, we need to move the pet
                if (memobj != null) {
                    PetObject petobj = _petMan.getPetObject(memobj.walkingId);
                    if (petobj != null) {
                        moveTo(petobj, sceneId, Integer.MAX_VALUE, portalId, destLoc,
                               new SceneMoveAdapter());
                    }
                }
            }
        };

        // now check to see if the destination scene is already hosted on a server
        Tuple<String, HostedRoom> nodeInfo = _peerMan.getSceneHost(sceneId);

        // if it's hosted on this server, then send the client directly to the scene
        if (nodeInfo != null && _peerMan.getNodeObject().nodeName.equals(nodeInfo.left)) {
            log.debug("Going directly to resolved local scene", "who" + mover.who(),
                      "sceneId", sceneId);
            resolveScene(sceneId, handler);
            return;
        }

        // if the mover is not a member, we don't allow server switches, so just fail
        if (memobj == null) {
            log.warning("Non-member requested move that requires server switch?",
                        "who", mover.who(), "info", nodeInfo);
            throw new InvocationException(RoomCodes.E_INTERNAL_ERROR);
        }

        // if it's hosted on another server; send the client to that server
        if (nodeInfo != null) {
            // first check access control on the remote scene
            HostedRoom hr = nodeInfo.right;
            if (memobj.canEnterScene(hr.placeId, hr.ownerId, hr.ownerType, hr.accessControl)) {
                log.debug("Going to remote node", "who", mover.who(),
                          "where", sceneId + "@" + nodeInfo.left);
                sendClientToNode(nodeInfo.left, memobj, listener);
            } else {
                listener.requestFailed(RoomCodes.E_ENTRANCE_DENIED);
            }
            return;
        }

        // otherwise the scene is not resolved here nor there; so we claim the scene by acquiring a
        // distributed lock and then resolve it locally
        _peerMan.acquireLock(MsoyPeerManager.getSceneLock(sceneId), new ResultListener<String>() {
            public void requestCompleted (String nodeName) {
                if (_peerMan.getNodeObject().nodeName.equals(nodeName)) {
                    log.debug("Got lock, resolving " + sceneId + ".");
                    resolveScene(sceneId, handler);
                } else if (nodeName != null) {
                    // some other peer got the lock before we could; send them there
                    log.debug("Didn't get lock, going remote " + sceneId + "@" + nodeName + ".");
                    sendClientToNode(nodeName, memobj, listener);
                } else {
                    log.warning("Scene lock acquired by null? [for=" + memobj.who() +
                                ", id=" + sceneId + "].");
                    listener.requestFailed(RoomCodes.INTERNAL_ERROR);
                }
            }
            public void requestFailed (Exception cause) {
                log.warning("Failed to acquire scene resolution lock", "for", memobj.who(),
                            "id", sceneId, cause);
                listener.requestFailed(RoomCodes.INTERNAL_ERROR);
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
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected FeedRepository _feedRepo;
}
