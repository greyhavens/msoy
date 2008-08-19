//
// $Id$

package com.threerings.msoy.room.server;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Implements the {@link WebRoomService}.
 */
public class WebRoomServlet extends MsoyServiceServlet
    implements WebRoomService
{
    // from interface WebRoomService
    public RoomInfo loadRoomInfo (int sceneId)
        throws ServiceException
    {
        try {
            SceneRecord screc = _sceneRepo.loadScene(sceneId);
            if (screc == null) {
                return null;
            }

            RoomInfo info = new RoomInfo();
            info.sceneId = screc.sceneId;
            info.name = screc.name;
            switch (screc.ownerType) {
            case MsoySceneModel.OWNER_TYPE_MEMBER:
                info.owner = _memberRepo.loadMemberName(screc.ownerId);
                break;
            case MsoySceneModel.OWNER_TYPE_GROUP:
                info.owner = _groupRepo.loadGroupName(screc.ownerId);
                break;
            }
            return info;

        } catch (PersistenceException pe) {
            log.warning("Load room info failed [sceneId=" + sceneId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WebRoomService
    public List<RoomInfo> loadMyRooms ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        try {
            List<SceneRecord> rooms = _sceneRepo.getOwnedScenes(mrec.memberId);
            return Lists.newArrayList(Iterables.transform(rooms,
                SceneRecord.TO_ROOM_INFO_WITH_THUMB));
        } catch (PersistenceException pe) {
            log.warning("Load rooms failed", "memberId", mrec.memberId, pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WebRoomService
    public RoomsResult loadGroupRooms (int groupId)
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser();

        try {
            RoomsResult result = new RoomsResult();

            // load up all scenes owned by this group
            List<SceneRecord> rooms = _sceneRepo.getOwnedScenes(
                MsoySceneModel.OWNER_TYPE_GROUP, groupId);
            result.groupRooms = Lists.newArrayList(
                Iterables.transform(rooms, SceneRecord.TO_ROOM_INFO));

            // load up all scenes owned by this member, filtering out their home
            Predicate<RoomInfo> notHome = new Predicate<RoomInfo>() {
                public boolean apply (RoomInfo info) {
                    return info.sceneId != mrec.homeSceneId;
                }
            };
            rooms = _sceneRepo.getOwnedScenes(mrec.memberId);
            result.callerRooms = Lists.newArrayList(
                Iterables.filter(Iterables.transform(rooms, SceneRecord.TO_ROOM_INFO), notHome));
            return result;

        } catch (PersistenceException pe) {
            log.warning("getGroupRooms failed [groupId=" + groupId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }
  
    // our dependencies
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GroupRepository _groupRepo;
}
