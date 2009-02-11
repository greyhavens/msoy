//
// $Id$

package com.threerings.msoy.room.server;

import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.data.all.RatingResult;

import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.admin.data.CostsConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.money.server.BuyResult;
import com.threerings.msoy.money.server.MoneyException;
import com.threerings.msoy.money.server.MoneyLogic;

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

        return _sceneRepo.getRatingRepository().rate(sceneId, mrec.memberId, rating).left;
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

        } else {
            // if viewing your own rooms, return a quote for buying a new one
            data.newRoomQuote = getRoomQuote(memberId);
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

        overview.winningRooms = loadDesignWinners();

        return overview;
    }

    // from interface WebRoomService
    public List<RoomInfo> loadDesignWinners ()
        throws ServiceException
    {
        // Fetch winners from server properties file; these are ordered starting with 1st place.
        Integer[] winnerSceneIds = ArrayUtils.toObject(ServerConfig.getContestWinningSceneIds());
        Iterable<SceneRecord> winners = _sceneRepo.loadScenes(Lists.newArrayList(winnerSceneIds));
        return Lists.newArrayList(Iterables.transform(winners, TO_ROOM_INFO));
    }

    // from interface WebRoomService
    public PurchaseResult<RoomInfo> purchaseRoom (Currency currency, int authedCost)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser();

        MoneyLogic.BuyOperation<RoomInfo> buyOp = new MoneyLogic.BuyOperation<RoomInfo>() {
            public boolean create (boolean magicFree, Currency currency, int amountPaid) {
                MessageBundle bundle = _serverMsgs.getBundle("server");
                String name = bundle.get("m.new_room_name", mrec.name);
                String portalAction = mrec.homeSceneId + ":" +
                    bundle.get("m.new_room_door", mrec.name);
                _newScene = _sceneRepo.createBlankRoom(
                    MsoySceneModel.OWNER_TYPE_MEMBER, mrec.memberId, name, portalAction, false);
                return true;
            }

            public RoomInfo getWare () {
                return _newScene.toRoomInfo();
            }

            protected SceneRecord _newScene;
        };

        BuyResult result;
        try {
            result = _moneyLogic.buyRoom(mrec, ROOM_PURCHASE_KEY, currency, authedCost,
                Currency.COINS, getRoomCoinCost(), buyOp);
        } catch (MoneyException me) {
            throw me.toServiceException();
        }
        if (result == null) {
            // this won't happen because our buyOp always returns true.
            log.warning("This isn't supposed to happen.");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        return new PurchaseResult<RoomInfo>(
            buyOp.getWare(), result.getBuyerBalances(), getRoomQuote(mrec.memberId));
    }

    /**
     * Get a price quote for a new room.
     */
    protected PriceQuote getRoomQuote (int memberId)
    {
        return _moneyLogic.securePrice(memberId, ROOM_PURCHASE_KEY,
            Currency.COINS, getRoomCoinCost());
    }

    /**
     * Return the current cost of a room, in coins.
     */
    protected int getRoomCoinCost ()
    {
        return _runtime.getCoinCost(CostsConfigObject.NEW_ROOM);
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

    /** An arbitrary key for tracking quotes for new rooms. */
    protected static final Object ROOM_PURCHASE_KEY = new Object();

    // our dependencies
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemberManager _memberMan;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected RuntimeConfig _runtime;
}
