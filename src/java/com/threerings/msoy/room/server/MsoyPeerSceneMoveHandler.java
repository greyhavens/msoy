/**
 *
 */
package com.threerings.msoy.room.server;

import static com.threerings.msoy.Log.log;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.Tuple;
import com.threerings.crowd.server.LocationManager;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.server.ThemeLogic;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeAvatarUseRecord;
import com.threerings.msoy.group.server.persist.ThemeRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.PetRepository;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.room.client.MsoySceneService.MsoySceneMoveListener;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyPortal;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.PetObject;
import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.server.MsoySceneRegistry.PeerSceneMoveHandler;
import com.threerings.msoy.room.server.MsoySceneRegistry.ThemeMoveHandler;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.world.server.WorldManager;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;
import com.threerings.whirled.client.SceneMoveAdapter;
import com.threerings.whirled.server.SceneManager;

/**
 * Handle all the complexities that happen on Whirled when you transition from one scene
 * to another, including the theme-boundary-crossing checks and manipulations.
 */
public class MsoyPeerSceneMoveHandler extends PeerSceneMoveHandler
{
    public MsoyPeerSceneMoveHandler (LocationManager locman, MsoyBodyObject mover,
        int sceneVer, int portalId, MsoyLocation destLoc, MsoySceneMoveListener listener)
    {
        super(locman, mover, sceneVer, listener);
        _msoyListener = listener;
        _portalId = portalId;
        _destLoc = destLoc;
        _mover = mover;
        _memobj = (mover instanceof MemberObject) ? (MemberObject)mover : null;
    }

    public void sceneOnNode (Tuple<String, HostedRoom> nodeInfo)
    {
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

        log.debug("Going to remote node", "who", _memobj.who(),
            "where", hr.placeId + "@" + nodeInfo.left);
        _screg.sendClientToNode(nodeInfo.left, _memobj, _msoyListener);
    }

    protected void effectSceneMove (SceneManager scmgr)
        throws InvocationException
    {
        final MsoyScene scene = (MsoyScene) scmgr.getScene();
        final RoomManager destmgr = (RoomManager)scmgr;

        // if we're not going to be let into the room, let our listener know now
        String accessMsg = scmgr.ratifyBodyEntry(_mover);
        if (accessMsg != null) {
            _msoyListener.requestFailed(accessMsg);
            return;
        }

        // if we're not a player (e.g. a pet or a mob), just let us through
        if ((_memobj == null) ||
                // Or, if we've already got an avatar quicklist and we're not crossing a theme
                // boundary, we can just finish the move as usual
                (_memobj.avatarCache != null && isInTheme(_memobj, scene.getThemeId()))) {
            finishMove(scene, destmgr);
            return;
        }

        // temporarily forbid anybody but subscribers to enter a theme from the mundane world
        if (scene.getThemeId() != 0 && !_memobj.tokens.isSubscriberPlus()) {
            _msoyListener.requestFailed(RoomCodes.E_ENTRANCE_DENIED);
            return;
        }

        // if we're walking a pet, find out what it is so we can test it for themeness
        _petobj = _petMan.getPetObject(_memobj.walkingId);

        // otherwise we need to take an extra trip over the invoker thread
        _invoker.postUnit(new ThemeRepositoryUnit(scene, new ThemeMoveHandler() {
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

        if (_petobj != null) {
            try {
                _screg.moveTo(_petobj, scene.getId(), Integer.MAX_VALUE, _portalId, _destLoc,
                    new MsoySceneMoveAdapter());

            } catch (InvocationException ie) {
                log.warning("Pet follow failed", "memberId", _memobj.getMemberId(),
                    "sceneId", scene.getId(), ie);
            }
        }
    }

    protected int _portalId;
    protected MsoyLocation _destLoc;
    protected MemberObject _memobj;
    protected MsoyBodyObject _mover;
    protected PetObject _petobj;
    protected MsoySceneMoveListener _msoyListener;

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MsoySceneRegistry _screg;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected PetRepository _petRepo;
    @Inject protected ThemeRepository _themeRepo;
    @Inject protected ThemeLogic _themeLogic;
    @Inject protected PetManager _petMan;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected WorldManager _worldMan;

    protected class ThemeRepositoryUnit extends RepositoryUnit
    {
        protected ThemeRepositoryUnit (
            MsoyScene scene, ThemeMoveHandler finishMove)
        {
            super("crossThemeBoundary");
            _listener = finishMove;

            _sceneId = scene.getId();
            _themeId = scene.getThemeId();
            _memberId = _memobj.getMemberId();
            _loadQuicklist = (_memobj.avatarCache == null || !isInTheme(_memobj, _themeId));
            _oldAvatarId = (_memobj.avatar != null) ? _memobj.avatar.itemId : 0;
            _candidateAvatarId = _oldAvatarId;
        }

        public void invokePersist ()
            throws Exception
        {
            if (_memberId == 0) {
                log.warning("What's going on? This user's memberId is zero!", "user", _memobj.who(),
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
                // if we have a pet and the pet is not stamped, it does not follow us
                if (_petobj != null &&
                        !_petRepo.isThemeStamped(_themeId, _petobj.pet.getMasterId())) {
                    _petobj = null;
                }

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

                // TODO: Enable this when it works.
                if (false && themeRec.playOnEnter) {
                    _gameId = groupRec.gameId;
                    if (_gameId == 0) {
                        log.warning("Play-on-enter theme has no game registered", "sceneId", _sceneId,
                            "themeGroupId", _themeId);
                    }
                }
            }

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
                List<AvatarRecord> ownedAvatars = avaRepo.getThemeAvatars(_memberId, _themeId);

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
            if (!_memobj.isActive()) {
                // the user may have logged out while we were fiddling
                return;
            }

            // if there's a lineup, instruct the client to show the selection UI and exit
            if (_lineup != null && !_lineup.isEmpty()) {
                _listener.selectGift(
                    _lineup.toArray(new Avatar[_lineup.size()]), _groupName.toString());
                return;
            }

            // else we're definitely moving
            _memobj.startTransaction();
            try {
                // if we loaded a quicklist (and it wasn't set during our thread hopping), set it
                if (_quicklist != null) {
                    _memobj.setAvatarCache(DSet.newDSet(
                        Lists.transform(_quicklist, new ItemRecord.ToItem<Avatar>())));
                }

                // if the theme really needs changing, do it
                if (!ObjectUtil.equals(_groupName, _memobj.theme)) {
                    _memobj.setTheme(_groupName);
                }
            } finally {
                _memobj.commitTransaction();
            }

            // kick off a DB write to persist the user's theme
            _invoker.postUnit(new RepositoryUnit("persistThemeId") {
                public void invokePersist () throws Exception {
                    _memberRepo.configureThemeId(_memberId, _themeId);
                }
                public void handleSuccess () { }
            });

            // if we're not switching avatars, we're done
            if (_candidateAvatarId == _oldAvatarId) {
                finishOrPunt();
                return;
            }

            // otherwise we have to route everything through MemberManager.setAvatar()
            _worldMan.setAvatar(_memobj, _candidateAvatarId, new WorldManager.SetAvatarListener() {
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

    protected static boolean isInTheme (MemberObject user, int groupId)
    {
        if (user.theme == null) {
            return groupId == 0;
        }
        return user.theme.getGroupId() == groupId;
    }


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
