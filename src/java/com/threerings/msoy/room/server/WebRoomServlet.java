//
// $Id$

package com.threerings.msoy.room.server;

import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.group.server.persist.GroupRepository;

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
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // our dependencies
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GroupRepository _groupRepo;
}
