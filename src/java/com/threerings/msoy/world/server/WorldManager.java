//
// $Id$

package com.threerings.msoy.world.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.group.server.persist.GroupRepository;

import static com.threerings.msoy.Log.log;

/**
 * Handles various global world services.
 */
@Singleton @EventThread
public class WorldManager
    implements WorldProvider
{
    @Inject public WorldManager (InvocationManager invmgr)
    {
        // register our bootstrap invocation service
        invmgr.registerDispatcher(new WorldDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    @Override // from interface WorldProvider
    public void getHomePageGridItems (
        ClientObject caller, InvocationService.ResultListener listener)
        throws InvocationException
    {
        MemberObject memObj = (MemberObject) caller;
        final MemberExperience[] experiences = new MemberExperience[memObj.experiences.size()];
        memObj.experiences.toArray(experiences);
        final boolean onTour = memObj.onTour;
        final int memberId = memObj.getMemberId();
        final short badgesVersion = memObj.getLocal(MemberLocal.class).badgesVersion;

        _invoker.postUnit(new PersistingUnit("getHPGridItems", listener, "who", memObj.who()) {
            @Override public void invokePersistent () throws Exception {
                _result = _memberLogic.getHomePageGridItems(
                    memberId, experiences, onTour, badgesVersion);
            }

            @Override public void handleSuccess () {
                reportRequestProcessed(_result);
            }

            protected Object _result;
        });
    }

    @Override // from interface WorldProvider
    public void getHomeId (final ClientObject caller, final byte ownerType, final int ownerId,
                           final InvocationService.ResultListener listener)
        throws InvocationException
    {
        _invoker.postUnit(new PersistingUnit("getHomeId", listener) {
            @Override public void invokePersistent () throws Exception {
                _homeId = _memberLogic.getHomeId(ownerType, ownerId);
            }
            @Override public void handleSuccess () {
                if (_homeId == null) {
                    handleFailure(new InvocationException("m.no_such_user"));
                } else {
                    reportRequestProcessed(_homeId);
                }
            }
            protected Integer _homeId;
        });
    }

    @Override // from interface WorldProvider
    public void setHomeSceneId (final ClientObject caller, final int ownerType, final int ownerId,
                                final int sceneId, final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject) caller;
        _invoker.postUnit(new PersistingUnit("setHomeSceneId", listener, "who", member.who()) {
            @Override public void invokePersistent () throws Exception {
                final int memberId = member.getMemberId();
                final SceneRecord scene = _sceneRepo.loadScene(sceneId);
                if (scene.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                    if (scene.ownerId == memberId) {
                        _memberRepo.setHomeSceneId(memberId, sceneId);
                    } else {
                        throw new InvocationException("e.not_room_owner");
                    }
                } else if (scene.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                    if (member.isGroupManager(scene.ownerId)) {
                        _groupRepo.setHomeSceneId(scene.ownerId, sceneId);
                    } else {
                        throw new InvocationException("e.not_room_manager");
                    }
                } else {
                    log.warning("Unknown scene model owner type [sceneId=" +
                        scene.sceneId + ", ownerType=" + scene.ownerType + "]");
                    throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
                }
            }
            @Override public void handleSuccess () {
                if (ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                    member.setHomeSceneId(sceneId);
                }
                super.handleSuccess();
            }
        });
    }

    // dependencies
    @Inject protected GroupRepository _groupRepo;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
}
