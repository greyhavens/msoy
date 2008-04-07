//
// $Id$

package com.threerings.msoy.world.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.data.SceneCodes;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomCodes;

import static com.threerings.msoy.Log.log;

/**
 * Handles some custom Whirled scene traversal business.
 */
public class MsoySceneRegistry extends SceneRegistry
    implements MsoySceneProvider
{
    public MsoySceneRegistry (InvocationManager invmgr, SceneRepository screp,
                              MsoyEventLogger eventLog)
    {
        super(invmgr, screp, new MsoySceneFactory(), new MsoySceneFactory());
        _eventLog = eventLog;

        // register our extra scene service
        invmgr.registerDispatcher(new MsoySceneDispatcher(this), SceneCodes.WHIRLED_GROUP);
    }

    /**
     * Called by the RoomManager a member updates a room.
     */
    public void memberUpdatedRoom (MemberObject user, final MsoyScene scene)
    {
        // publish to this member's feed that they updated their room
        final int memId = user.getMemberId();
        String uname = "publishRoomUpdate[id=" + memId + ", scid=" + scene.getId() + "]";
        MsoyServer.invoker.postUnit(new RepositoryUnit(uname) {
            public void invokePersist () throws PersistenceException {
                MsoyServer.feedRepo.publishMemberMessage(
                    memId, FeedMessageType.FRIEND_UPDATED_ROOM,
                    String.valueOf(scene.getId()) + "\t" + scene.getName());
            }
            public void handleSuccess () {
                // nada
            }
        });

        // record this edit to the grindy log
        _eventLog.roomUpdated(memId, scene.getId());
    }

    // from interface MsoySceneProvider
    public void moveTo (ClientObject caller, final int sceneId, int version, int portalId,
                        MsoyLocation destLoc, final SceneService.SceneMoveListener listener)
        throws InvocationException
    {
        final MemberObject memobj = (MemberObject)caller;

        // if they are departing a scene hosted by this server, move them to the exit; if we fail
        // later, they will have walked to the exit and then received an error message, alas
        RoomManager srcmgr = (RoomManager)getSceneManager(memobj.getSceneId());
        if (srcmgr != null) {
            // give the source scene manager a chance to do access control
            Portal dest = ((MsoyScene)srcmgr.getScene()).getPortal(portalId);
            if (dest != null) {
                String errmsg = srcmgr.mayTraversePortal(memobj, dest);
                if (errmsg != null) {
                    throw new InvocationException(errmsg);
                }
                srcmgr.willTraversePortal(memobj, dest);
            }
        }

        // if this member has followers, tell them all to make the same scene move
        for (MemberName follower :
                 memobj.followers.toArray(new MemberName[memobj.followers.size()])) {
            MemberObject folobj = MsoyServer.lookupMember(follower.getMemberId());
            // if they've logged off or are no longer following us, remove them from our set
            if (folobj == null || folobj.following == null ||
                !folobj.following.equals(memobj.memberName)) {
                log.info("Clearing departed follower " + follower + ".");
                memobj.removeFromFollowers(follower.getMemberId());
                continue;
            }
            folobj.postMessage(RoomCodes.FOLLOWEE_MOVED, sceneId);
        }

        // now check to see if the destination scene is already hosted on a server
        Tuple<String, HostedRoom> nodeInfo = MsoyServer.peerMan.getSceneHost(sceneId);

        // if it's hosted on this server, then send the client directly to the scene
        if (nodeInfo != null && MsoyServer.peerMan.getNodeObject().nodeName.equals(nodeInfo.left)) {
            log.info("Going directly to resolved local scene " + sceneId + ".");
            resolveScene(sceneId, new MsoySceneMoveHandler(memobj, version, destLoc, listener));
            return;
        }

        // if it's hosted on another server; send the client to that server
        if (nodeInfo != null) {
            // first check access control on the remote scene
            HostedRoom room = nodeInfo.right;
            boolean hasRights = memobj.canEnterScene(room.ownerId, room.ownerType, 
                                                     room.accessControl);
            if (!hasRights && memobj.tokens.isSupport()) {
                log.info("Allowing support+ to enter scene which they otherwise couldn't enter " +
                    "[sceneId=" + sceneId + ", ownerId=" + room.ownerId + ", ownerType" + 
                    room.ownerType + ", accessControl=" + room.accessControl + "].");
                hasRights = true;
            }

            if (hasRights) {
                log.info("Going to remote node " + sceneId + "@" + nodeInfo.left + ".");
                
                // flush all metrics, because the memobj will get serialized *before* the room exit is processed
                memobj.metrics.save(memobj);
                
                sendClientToNode(nodeInfo.left, memobj, listener);
            } else {
                listener.requestFailed(RoomCodes.E_ENTRANCE_DENIED);
            }
            return;
        }

        // otherwise the scene is not resolved here nor there; so we claim the scene by acquiring a
        // distributed lock and then resolve it locally
        final MsoySceneMoveHandler handler =
            new MsoySceneMoveHandler(memobj, version, destLoc, listener);
        MsoyServer.peerMan.acquireLock(
            MsoyPeerManager.getSceneLock(sceneId), new ResultListener<String>() {
            public void requestCompleted (String nodeName) {
                if (MsoyServer.peerMan.getNodeObject().nodeName.equals(nodeName)) {
                    log.info("Got lock, resolving " + sceneId + ".");
                    resolveScene(sceneId, handler);
                } else if (nodeName != null) {
                    // some other peer got the lock before we could; send them there
                    log.info("Didn't get lock, going remote " + sceneId + "@" + nodeName + ".");
                    sendClientToNode(nodeName, memobj, listener);
                } else {
                    log.warning("Scene lock acquired by null? [for=" + memobj.who() +
                                ", id=" + sceneId + "].");
                    listener.requestFailed(RoomCodes.INTERNAL_ERROR);
                }
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Failed to acquire scene resolution lock " +
                        "[for=" + memobj.who() + ", id=" + sceneId + "].", cause);
                listener.requestFailed(RoomCodes.INTERNAL_ERROR);
            }
        });
    }

    protected void sendClientToNode (String nodeName, MemberObject memobj,
                                     SceneService.SceneMoveListener listener)
    {
        String hostname = MsoyServer.peerMan.getPeerPublicHostName(nodeName);
        int port = MsoyServer.peerMan.getPeerPort(nodeName);
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
        MsoyServer.peerMan.forwardMemberObject(nodeName, memobj);
    }

    protected MsoyEventLogger _eventLog;
}
