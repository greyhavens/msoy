//
// $Id$

package com.threerings.msoy.room.server;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.Tuple;

import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.web.gwt.RatingResult;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.gwt.RoomDetail;
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
    public RoomDetail loadRoomDetail (int sceneId)
        throws ServiceException
    {
        SceneRecord screc = _sceneRepo.loadScene(sceneId);
        if (screc == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        MemberRecord mrec = getAuthedUser();

        RoomDetail detail = screc.toRoomDetail();
        switch (screc.ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            detail.owner = _memberRepo.loadMemberName(screc.ownerId);
            break;
        case MsoySceneModel.OWNER_TYPE_GROUP:
            detail.owner = _groupRepo.loadGroupName(screc.ownerId);
            break;
        }
        if (mrec != null) {
            detail.memberRating =
                _sceneRepo.getRatingRepository().getRating(sceneId, mrec.memberId);
        }
        return detail;
    }

    public RatingResult rateRoom (int sceneId, byte rating)
        throws ServiceException
    {
        SceneRecord screc = _sceneRepo.loadScene(sceneId);
        if (screc == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        MemberRecord mrec = requireAuthedUser();

        Tuple<RatingRepository.RatingAverageRecord, Boolean> result =
            _sceneRepo.getRatingRepository().rate(sceneId, mrec.memberId, rating);

        return new RatingResult(result.left.average, result.left.count);
    }

    // from interface WebRoomService
    public MemberRoomsResult loadMemberRooms (int memberId)
        throws ServiceException
    {
        MemberRecord mrec = _memberRepo.loadMember(memberId);
        if (mrec == null) {
            log.warning("Could not locate member when loading rooms", "memberId", memberId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        MemberRecord reqrec = getAuthedUser();
        MemberRoomsResult data = new MemberRoomsResult();
        data.owner = mrec.getName();
        Iterable<SceneRecord> riter = _sceneRepo.getOwnedScenes(memberId);
        // hide locked rooms from other members (even from friends)
        if (reqrec == null || reqrec.memberId != memberId) {
            riter = Iterables.filter(riter, IS_PUBLIC);
        }
        data.rooms = Lists.newArrayList(Iterables.transform(riter, TO_ROOM_INFO));
        return data;
    }

    // from interface WebRoomService
    public RoomsResult loadGroupRooms (int groupId)
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser();
        RoomsResult result = new RoomsResult();

        // load up all scenes owned by this group
        List<SceneRecord> rooms = _sceneRepo.getOwnedScenes(
            MsoySceneModel.OWNER_TYPE_GROUP, groupId);
        result.groupRooms = Lists.newArrayList(Iterables.transform(rooms, TO_ROOM_INFO));

        // load up all scenes owned by this member, filtering out their home
        Predicate<SceneRecord> notHome = new Predicate<SceneRecord>() {
            public boolean apply (SceneRecord rec) {
                return rec.sceneId != mrec.homeSceneId;
            }
        };
        rooms = _sceneRepo.getOwnedScenes(mrec.memberId);
        result.callerRooms = Lists.newArrayList(
            Iterables.transform(Iterables.filter(rooms, notHome), TO_ROOM_INFO));

        return result;
    }

    // from interface WebRoomService
    public OverviewResult loadOverview ()
        throws ServiceException
    {
        OverviewResult overview = new OverviewResult();

        // The scene IDs of the current N most populated rooms
        Iterable<Integer> activeIds =
            Iterables.transform(Iterables.limit(_memberMan.getPPSnapshot().getTopScenes(), 20),
                TO_SCENE_ID);

        // Load up the records for each scene ID
        List<SceneRecord> activeRooms = _sceneRepo.loadScenes(Lists.newArrayList(activeIds));

        overview.activeRooms = Lists.newArrayList(Iterables.transform(activeRooms, TO_ROOM_INFO));

        Iterable<SceneRecord> cool = _sceneRepo.loadScenes(0, 20);
        overview.coolRooms = Lists.newArrayList(Iterables.transform(cool, TO_ROOM_INFO));

        return overview;
    }

    protected static final Predicate<SceneRecord> IS_PUBLIC = new Predicate<SceneRecord>() {
        public boolean apply (SceneRecord room) {
            return room.accessControl == MsoySceneModel.ACCESS_EVERYONE;
        }
    };

    protected Function<SceneRecord,RoomInfo> TO_ROOM_INFO = new Function<SceneRecord,RoomInfo>() {
        public RoomInfo apply (SceneRecord record) {
            RoomInfo info = record.toRoomInfo();
            PopularPlacesSnapshot.Place card = _memberMan.getPPSnapshot().getScene(record.sceneId);
            if (card != null) {
                info.population = card.population;
            }
            return info;
        }
    };

    protected Function<PopularPlacesSnapshot.Place,Integer> TO_SCENE_ID =
        new Function<PopularPlacesSnapshot.Place,Integer>() {
            public Integer apply (PopularPlacesSnapshot.Place place) {
                return place.placeId;
            }
        };

    // our dependencies
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemberManager _memberMan;
}
