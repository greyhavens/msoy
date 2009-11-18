//
// $Id$

package com.threerings.msoy.room.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.util.Name;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DSet;
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
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.world.server.WorldManager;

import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.group.server.ThemeLogic;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeAvatarUseRecord;
import com.threerings.msoy.group.server.persist.ThemeRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.room.client.MsoySceneService.MsoySceneMoveListener;
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
                            releaseLock();
                            listener.sceneWasResolved(scmgr);
                        }
                        public void sceneFailedToResolve (int sceneId, Exception reason) {
                            releaseLock();
                            listener.sceneFailedToResolve(sceneId, reason);
                        }
                        protected void releaseLock () {
                            _peerMan.releaseLock(MsoyPeerManager.getSceneLock(sceneId),
                                new ResultListener.NOOP<String>());
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

    /**
     * Reclaim an item out of a scene on behalf of the specified member.
     */
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

    /**
     * Transfer room ownership.
     */
    public void transferOwnership (
        final int sceneId, final byte ownerType, final int ownerId, final Name ownerName,
        final boolean lockToOwner, final ResultListener<Void> listener)
    {
        resolvePeerScene(sceneId, new PeerSceneResolutionListener() {
            public void sceneWasResolved (SceneManager scmgr) {
                ((RoomManager)scmgr).transferOwnership(ownerType, ownerId, ownerName, lockToOwner);
                listener.requestCompleted(null);
            }

            public void sceneOnNode (Tuple<String, HostedRoom> nodeInfo) {
                _peerMan.transferRoomOwnership(nodeInfo.left, sceneId,
                    ownerType, ownerId, ownerName, lockToOwner, listener);
            }

            public void sceneFailedToResolve (int sceneId, Exception reason) {
                listener.requestFailed(reason);
            }
            });
    }

    // TODO: the other version of moveTo() needs to also become peer-aware

    // from interface MsoySceneProvider
    public void moveTo (ClientObject caller, int sceneId, int version, int portalId,
                        MsoyLocation destLoc, MsoySceneMoveListener listener)
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
        resolvePeerScene(sceneId, new MsoyPeerSceneMoveHandler(
            _locman, mover, version, portalId, destLoc, listener));
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

    protected boolean isInTheme (MemberObject user, int groupId)
    {
        if (user.theme == null) {
            return groupId == 0;
        }
        return user.theme.getGroupId() == groupId;
    }

    protected interface ThemeMoveHandler
    {
        void finish ();
        void puntToGame (int gameId);
        void selectGift (Avatar[] avatars, String groupName);
    }

    protected class MsoyPeerSceneMoveHandler extends PeerSceneMoveHandler
    {
        protected MsoyPeerSceneMoveHandler (LocationManager locman, MsoyBodyObject mover,
            int sceneVer, int portalId, MsoyLocation destLoc, MsoySceneMoveListener listener)
        {
            super(locman, mover, sceneVer, listener);
            _msoyListener = listener;
            _portalId = portalId;
            _destLoc = destLoc;
            _mover = mover;
            _memobj = (mover instanceof MemberObject) ? (MemberObject)mover : null;
        }

        public void sceneOnNode (Tuple<String, HostedRoom> nodeInfo) {
            if (_memobj == null) {
                log.warning("Non-member requested move that requires server switch?",
                    "who", _mover.who(), "info", nodeInfo);
                _listener.requestFailed(RoomCodes.E_INTERNAL_ERROR);
                return;
            }

            // investigate the remote scene
            HostedRoom hr = nodeInfo.right;

            // check for access control
            if (!_memobj.canEnterScene(hr.placeId, hr.ownerId, hr.ownerType, hr.accessControl,
                _memobj.getLocal(MemberLocal.class).friendIds)) {
                _listener.requestFailed(RoomCodes.E_ENTRANCE_DENIED);
                return;
            }

            log.debug("Going to remote node", "who", _mover.who(),
                "where", hr.placeId + "@" + nodeInfo.left);
            sendClientToNode(nodeInfo.left, _memobj, _msoyListener);
        }

        protected void effectSceneMove (SceneManager scmgr) throws InvocationException {
            final MsoyScene scene = (MsoyScene) scmgr.getScene();
            final RoomManager destmgr = (RoomManager)scmgr;

            // if we're not going to be let into the room, let our listener know now
            String accessMsg = scmgr.ratifyBodyEntry(_memobj);
            if (accessMsg != null) {
                _msoyListener.requestFailed(accessMsg);
                return;
            }

            // if we've already got an avatar quicklist and we're not crossing a theme
            // boundary, we can just finish the move as usual
            if (_memobj.avatarCache != null && isInTheme(_memobj, scene.getThemeId())) {
                finishMove(scene, destmgr);
                return;
            }

            // temporarily forbid anybody but subscribers to enter a theme from the mundane world
            if (_memobj.theme == null && scene.getThemeId() != 0 &&
                    !_memobj.tokens.isSubscriberPlus()) {
                _msoyListener.requestFailed(InvocationCodes.E_ACCESS_DENIED);
            }

            // otherwise we need to take an extra trip over the invoker thread
            _invoker.postUnit(new ThemeRepositoryUnit(scene, _memobj, new ThemeMoveHandler() {
                public void finish () {
                    finishMove(scene, destmgr);
                }
                public void puntToGame (int gameId) {
                    _msoyListener.moveToBeHandledByAVRG(gameId, scene.getId());
                }
                public void selectGift (Avatar[] avatars, String groupName) {
                    _msoyListener.selectGift(avatars, groupName);
                }
            }));
        }

        protected void finishMove (MsoyScene scene, RoomManager destmgr)
        {
            // create a fake "from" portal that contains our destination location
            MsoyPortal from = new MsoyPortal();
            from.targetPortalId = (short)-1;
            from.dest = _destLoc;

            // let the destination room manager know that we're coming in "from" that portal
            destmgr.mapEnteringBody(_mover, from);

            try {
                MsoyPeerSceneMoveHandler.super.effectSceneMove(destmgr);

            } catch (InvocationException ie) {
                // if anything goes haywire, clear out our entering status
                destmgr.clearEnteringBody(_mover);
                log.warning("Scene move failed", "mover", _mover.who(),
                    "sceneId", scene.getId(), ie);
                _msoyListener.requestFailed(ie.getMessage());
                return;
            }

            // for members, check for following entities
            if (_memobj != null) {
                // deal with pets
                PetObject petobj = _petMan.getPetObject(_memobj.walkingId);
                if (petobj != null) {
                    try {
                        moveTo(petobj, scene.getId(), Integer.MAX_VALUE, _portalId, _destLoc,
                            new MsoySceneMoveAdapter());

                    } catch (InvocationException ie) {
                        log.warning("Pet follow failed", "memberId", _memobj.getMemberId(),
                            "sceneId", scene.getId(), ie);
                    }
                }
            }
        }

        protected int _portalId;
        protected MsoyLocation _destLoc;
        protected MemberObject _memobj;
        protected MsoyBodyObject _mover;
        protected MsoySceneMoveListener _msoyListener;
    }

    protected class ThemeRepositoryUnit extends RepositoryUnit
    {
        protected ThemeRepositoryUnit (
            MsoyScene scene, MemberObject user, ThemeMoveHandler finishMove)
        {
            super("crossThemeBoundary");
            _user = user;
            _listener = finishMove;

            _sceneId = scene.getId();
            _themeId = scene.getThemeId();
            _memberId = user.getMemberId();
            _loadQuicklist = (_user.avatarCache == null || !isInTheme(user, _themeId));
            _oldAvatarId = (_user.avatar != null) ? _user.avatar.itemId : 0;
            _candidateAvatarId = _oldAvatarId;
        }

        public void invokePersist ()
            throws Exception
        {
            if (_memberId == 0) {
                log.warning("What's going on? This user's memberId is zero!", "user", _user.who(),
                    "sceneId", _sceneId, "themeId", _themeId);
                return;
            }
            AvatarRepository avaRepo = _itemLogic.getAvatarRepository();

            // if we're moving into a theme, or we just don't have an avatar cache yet, load it
            if (_loadQuicklist) {
                // reload the avatar cache from recently-touched items in repo
                _quicklist = avaRepo.loadRecentlyTouched(
                    _memberId, _themeId, MemberObject.AVATAR_CACHE_SIZE);
            }

            if (_themeId != 0) {
                ThemeRecord themeRec = _themeRepo.loadTheme(_themeId);
                if (themeRec == null) {
                    // internal error, log it and let the move complete
                    log.warning("Couldn't find theme record for scene", "sceneId", _sceneId,
                        "themeGroupId", _themeId);
                    return;
                }

                GroupRecord groupRec = _groupRepo.loadGroup(_themeId);
                if (groupRec == null) {
                    // internal error, log it and let the move complete
                    log.warning("Couldn't find group record for scene theme", "sceneId", _sceneId,
                        "themeGroupId", _themeId);
                    return;
                }

                _groupName = groupRec.toGroupName();

                if (themeRec.playOnEnter) {
                    _gameId = groupRec.gameId;
                    if (_gameId == 0) {
                        log.warning("Play-on-enter theme has no game registered", "sceneId", _sceneId,
                            "themeGroupId", _themeId);
                    }
                }
            }

            // if we're definitely going, update MemberRecord (even with _themeId == 0)
            _memberRepo.configureThemeId(_memberId, _themeId);

            // if we've been in this theme before, see what we wore last
            ThemeAvatarUseRecord aRec = _themeRepo.getLastWornAvatar(_memberId, _themeId);
            if (aRec != null && aRec.itemId != _candidateAvatarId) {
                _candidateAvatarId = aRec.itemId;
            }

            // if at this point our avatar (either current or most recently worn for theme) is
            // the ghost or not, in fact, stamped for the theme, we have to do more work
            if (_themeId != 0 &&
                    (_candidateAvatarId == 0 ||
                     !avaRepo.isThemeStamped(_themeId, _candidateAvatarId))) {
                // see if the player has any existing acceptable avatars in their inventory
                List<AvatarRecord> ownedAvatars = avaRepo.findItems(_memberId, null, _themeId);

                if (ownedAvatars.size() > 0) {
                    // if so, pick a random one
                    int rndIx = RandomUtil.getInt(ownedAvatars.size());
                    _candidateAvatarId = ownedAvatars.get(rndIx).itemId;

                } else {
                    // otherwise, fire up the selection UI
                    _lineup = _themeLogic.loadLineup(_themeId, 0, 6);
                    _candidateAvatarId = 0;
                }
            }
        }

        public void handleSuccess ()
        {
            if (!_user.isActive()) {
                // the user may have logged out while we were fiddling
                return;
            }

            _user.startTransaction();
            try {
                // if we loaded a quicklist (and it wasn't set during our thread hopping), set it
                if (_quicklist != null) {
                    _user.setAvatarCache(DSet.newDSet(
                        Lists.transform(_quicklist, new ItemRecord.ToItem<Avatar>())));
                }

                // if the theme really needs changing, do it
                if (!ObjectUtil.equals(_groupName, _user.theme)) {
                    _user.setTheme(_groupName);
                }
            } finally {
                _user.commitTransaction();
            }

            // if there's a lineup, instruct the client to show the selection UI and exit
            if (_lineup != null && !_lineup.isEmpty()) {
                _listener.selectGift(
                    _lineup.toArray(new Avatar[_lineup.size()]), _groupName.toString());
                return;
            }

            // if we're not switching avatars, we're done
            if (_candidateAvatarId == _oldAvatarId) {
                finishOrPunt();
                return;
            }

            // otherwise we have to route everything through MemberManager.setAvatar()
            _worldMan.setAvatar(_user, _candidateAvatarId, new WorldManager.SetAvatarListener() {
                public void success () {
                    finishOrPunt();
                }
                public void accessDeniedFailure () throws Exception {
                    // let's reluctantly accept this until we see if it happens for real
                    finishOrPunt();
                }
                public void noSuchItemFailure () throws Exception {
                    // let's reluctantly accept this until we see if it happens for real
                    finishOrPunt();
                }
            });
        }

        protected void finishOrPunt ()
        {
            if (_gameId != 0) {
                _listener.puntToGame(_gameId);
            } else {
                _listener.finish();
            }
        }

        protected MemberObject _user;
        protected ThemeMoveHandler _listener;

        // members that mainly communicate dobj thread from the constructor to invokePersist()
        protected int _sceneId;
        protected int _themeId;
        protected int _memberId;
        protected boolean _loadQuicklist;
        protected int _oldAvatarId;

        // members that mainly communicate from invokePersist() to handleSuccess()
        protected int _candidateAvatarId;
        protected List<AvatarRecord> _quicklist;
        protected List<Avatar> _lineup;
        protected GroupName _groupName;

        // for finishOrPunt()
        protected int _gameId;
    }

    // our dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ThemeRepository _themeRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemberLocator _locator;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected ThemeLogic _themeLogic;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected PetManager _petMan;
    @Inject protected WorldManager _worldMan;

    /**
     * Implements MsoySceneMoveListener trivially.
     */
    protected static class MsoySceneMoveAdapter extends SceneMoveAdapter
        implements MsoySceneMoveListener
    {
        public void moveToBeHandledByAVRG (int gameId, int sceneId) {
            // noop
        }
        public void selectGift (Avatar[] avatars, String groupName) {
            // noop
        }
    }
}
