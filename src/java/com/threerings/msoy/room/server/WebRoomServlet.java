//
// $Id$

package com.threerings.msoy.room.server;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.commons.lang.ArrayUtils;

import com.samskivert.util.ServiceWaiter.TimeoutException;

import com.samskivert.servlet.util.ServiceWaiter;

import com.threerings.presents.peer.server.NodeRequestsListener;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.admin.data.CostsConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.server.ThemeLogic;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeHomeTemplateRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.gwt.RoomDetail;
import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot.Place;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.server.MsoyServiceServlet;

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
            detail.mayManage = (mrec != null) && (mrec.memberId == screc.ownerId);
            break;
        case MsoySceneModel.OWNER_TYPE_GROUP:
            detail.owner = _groupRepo.loadGroupName(screc.ownerId);
            detail.mayManage = (mrec != null) &&
                (Rank.MANAGER == _groupRepo.getMembership(screc.ownerId, mrec.memberId).left);
            break;
        }
        if (mrec != null) {
            detail.memberRating =
                _sceneRepo.getRatingRepository().getRating(sceneId, mrec.memberId);
        }
        if (screc.themeGroupId != 0) {
            detail.theme = _groupRepo.loadGroupName(screc.themeGroupId);
            detail.isTemplate =
                (_themeRepo.loadHomeTemplate(screc.themeGroupId, sceneId) != null);
        }
        return detail;
    }

    // from interface WebRoomService
    public void canGiftRoom (int sceneId)
        throws ServiceException
    {
        _roomLogic.checkCanGiftRoom(requireAuthedUser(), sceneId);
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
        if (reqrec == null || (reqrec.memberId != memberId && !reqrec.isSupport())) {
            riter = Iterables.filter(riter, IS_PUBLIC);

        } else if (reqrec.memberId == memberId) {
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
        List<Place> top20Scenes = Lists.newArrayListWithCapacity(20);
        Iterator<Place> allTopScenes = _memberMan.getPPSnapshot().getTopScenes().iterator();
        for (int ii = 0; ii < 20 && allTopScenes.hasNext(); ii++) {
            top20Scenes.add(allTopScenes.next());
        }
        Iterable<Integer> activeIds = Iterables.transform(top20Scenes, TO_SCENE_ID);
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
        return _moneyLogic.buyRoom(
            mrec, ROOM_PURCHASE_KEY, currency, authedCost, Currency.COINS, getRoomCoinCost(),
            new MoneyLogic.BuyOperation<RoomInfo>() {
            public RoomInfo create (boolean magicFree, Currency currency, int amountPaid) {
                String name = _serverMsgs.getBundle("server").get("m.new_room_name", mrec.name);
                return _sceneLogic.createBlankRoom(MsoySceneModel.OWNER_TYPE_MEMBER, mrec.memberId,
                    SceneRecord.Stock.EXTRA_MEMBER_ROOM.getSceneId(), true, 0, name, null).toRoomInfo();
            }
        }).toPurchaseResult();
    }

    // from interface WebRoomService
    public void stampRoom (int sceneId, int groupId, boolean doStamp)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        SceneRecord sceneRec = _sceneRepo.loadScene(sceneId);
        if (sceneRec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        if (sceneRec.themeGroupId != (doStamp ? 0 : groupId)) {
            log.warning("Unexpected scene theme", "scene", sceneId, "theme", groupId,
                "scene.theme", sceneRec.themeGroupId, "doStamp", doStamp);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ensureSceneManager(mrec, sceneRec);

        // all is well, let's go ahead
        if (_sceneRepo.stampRoom(sceneId, doStamp ? groupId : 0)) {
            // if the scene is resolved somewhere, nuke it
            _sceneActions.evictAndShutdown(sceneId);

            // if we unstamped a room, make sure it's not a home room template
            _themeRepo.removeHomeTemplate(groupId, sceneId);

        } else {
            log.warning("No room was stamped!", "sceneId", sceneId, "groupId", groupId);
            // let it go
        }
    }

    // from interface WebRoomService
    public void makeTemplate (final int sceneId, final int groupId, boolean doMake)
        throws ServiceException
    {
        MemberRecord mrec = requireThemeManager(groupId);

        SceneRecord sceneRec = _sceneRepo.loadScene(sceneId);
        if (sceneRec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        if (sceneRec.themeGroupId != groupId) {
            log.warning("Unexpected scene theme", "scene", sceneId, "theme", groupId,
                "scene.theme", sceneRec.themeGroupId, "doMake", doMake);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ensureSceneManager(mrec, sceneRec);

        if (doMake) {
            // if we get this far, we need to flush out any pending furniture changes to
            // disk before continuing on with the checks. this necessitates a fair bit of
            // frustrating thread bouncing.
            final ServiceWaiter<Void> waiter = new ServiceWaiter<Void>();

            if (mrec.isSupport()) {
                // Allow support staff unrestricted template creation
                waiter.requestCompleted(null);
            } else {
                _sceneActions.flushUpdates(sceneId, new NodeRequestsListener<Void>() {
                    @Override public void requestsProcessed (NodeRequestsResult<Void> result) {
                        _sceneLogic.validateAllTemplateFurni(groupId, sceneId, waiter);
                    }
                    @Override public void requestFailed (String cause) {
                        // the flush failed, try the validation anyway
                        _sceneLogic.validateAllTemplateFurni(groupId, sceneId, waiter);
                    }
                });
            }
            try {
                if (!waiter.waitForResponse()) {
                    throw new ServiceException(waiter.getError().getMessage());
                }
                _themeRepo.setHomeTemplate(groupId, sceneId);

            } catch (TimeoutException e) {
                log.warning("Flush waiter timed out", e);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

        } else {
            _themeRepo.removeHomeTemplate(groupId, sceneId);
        }
    }

    public TemplatesResult loadThemeTemplates (int groupId)
        throws ServiceException
    {
        requireThemeManager(groupId);

        List<SceneRecord> scenes = _sceneRepo.loadScenes(Lists.transform(
            _themeRepo.loadHomeTemplates(groupId), ThemeHomeTemplateRecord.TO_SCENE_ID));

        TemplatesResult result = new TemplatesResult();
        result.groupRooms = Lists.newArrayList(Lists.transform(scenes, TO_ROOM_INFO));

        return result;
    }

    protected void ensureSceneManager (MemberRecord mrec, SceneRecord sceneRec)
        throws ServiceException
    {
        // make sure we're allowed to stamp this room
        if (mrec.isSupport()) {
            // support always may
            return;
        }

        boolean mayManage;
        if (sceneRec.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
            // if the scene belongs to a member, the member is the admin
            mayManage = (mrec.memberId == sceneRec.ownerId);
        } else {
            // a group room is managed by any group manager
            mayManage = (Rank.MANAGER ==
                _groupRepo.getMembership(sceneRec.ownerId, mrec.memberId).left);
        }
        if (!mayManage) {
            log.warning("User not allowed to stamp this scene", "scene", sceneRec.sceneId,
                "who", mrec.who());
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }
    }

    protected MemberRecord requireThemeManager (int groupId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure we're allowed to stamp for this theme
        if (!_themeLogic.isTheme(groupId) ||
                _groupRepo.getMembership(groupId, mrec.memberId).left != Rank.MANAGER) {
            log.warning("User not allowed to manage this theme",
                "theme", groupId, "who", mrec.who());
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }
        return mrec;
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
                info.hopping = card.hopping;
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
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberManager _memberMan;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected RoomLogic _roomLogic;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected SceneLogic _sceneLogic;
    @Inject protected SceneNodeActions _sceneActions;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected ThemeLogic _themeLogic;
    @Inject protected ThemeRepository _themeRepo;
}
