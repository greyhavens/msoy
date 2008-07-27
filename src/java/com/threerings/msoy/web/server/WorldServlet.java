//
// $Id$

package com.threerings.msoy.web.server;

import java.net.URLEncoder;
import java.sql.Timestamp;

import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ExpiringReference;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.server.ChatChannelManager;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.gwt.ShopData;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.GameDetailRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;
import com.threerings.msoy.world.server.persist.SceneRecord;

import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.gwt.FeaturedGameInfo;
import com.threerings.msoy.game.gwt.LaunchConfig;
import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.game.xml.MsoyGameParser;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.person.data.FeedMessage;
import com.threerings.msoy.person.server.persist.FeedRepository;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.WorldService;
import com.threerings.msoy.web.data.GalaxyData;
import com.threerings.msoy.web.data.GroupCard;
import com.threerings.msoy.web.data.LandingData;
import com.threerings.msoy.web.data.MyWhirledData;
import com.threerings.msoy.web.data.PlaceCard;
import com.threerings.msoy.web.data.RoomInfo;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Implements the {@link WorldService}.
 */
public class WorldServlet extends MsoyServiceServlet
    implements WorldService
{
    // from interface WorldService
    public LandingData getLandingData ()
        throws ServiceException
    {
        LandingData data = ExpiringReference.get(_landingData);
        if (data != null) {
            return data;
        }

        try {
            data = new LandingData();

            // determine our featured whirled based on who's online now
            PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
            List<GroupCard> popWhirleds = Lists.newArrayList();
            for (PlaceCard card : pps.getTopWhirleds()) {
                GroupRecord group = _groupRepo.loadGroup(card.placeId);
                if (group != null) {
                    GroupCard gcard = group.toGroupCard();
                    gcard.population = card.population;
                    popWhirleds.add(gcard);
                    if (popWhirleds.size() == GalaxyData.FEATURED_WHIRLED_COUNT) {
                        break;
                    }
                }
            }
            // if we don't have enough people online, supplement with other groups
            if (popWhirleds.size() < GalaxyData.FEATURED_WHIRLED_COUNT) {
                int count = GalaxyData.FEATURED_WHIRLED_COUNT - popWhirleds.size();
                for (GroupRecord group : _groupRepo.getGroupsList(0, count)) {
                    popWhirleds.add(group.toGroupCard());
                }
            }
            data.featuredWhirleds = popWhirleds.toArray(new GroupCard[popWhirleds.size()]);

            // determine the "featured" games
            data.topGames = _gameLogic.loadTopGames(pps);

            // select the top rated avatars
            ItemRepository<ItemRecord, ?, ?, ?> repo =
                _itemMan.getRepository(Item.AVATAR);
            List<ListingCard> cards = Lists.newArrayList();
            for (CatalogRecord crec : repo.loadCatalog(CatalogQuery.SORT_BY_RATING, false, null, 0,
                                                       0, null, 0, ShopData.TOP_ITEM_COUNT)) {
                cards.add(crec.toListingCard());
            }
            _itemLogic.resolveCardNames(cards);
            data.topAvatars = cards.toArray(new ListingCard[cards.size()]);

            _landingData = ExpiringReference.create(data, LANDING_DATA_EXPIRY);
            return data;

        } catch (PersistenceException pe) {
            log.warning("Failed to load landing data.", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    protected FeaturedGameInfo toFeaturedGameInfo (
        GameRecord game, GameDetailRecord detail, int pop)
        throws PersistenceException
    {
        FeaturedGameInfo info = (FeaturedGameInfo)game.toGameInfo(new FeaturedGameInfo());
        info.avgDuration = detail.getAverageDuration();
        int[] players = getMinMaxPlayers((Game)game.toItem());
        info.minPlayers = players[0];
        info.maxPlayers = players[1];
        info.playersOnline = pop;
        info.creator = _memberRepo.loadMemberName(game.creatorId);
        return info;
    }

    protected int[] getMinMaxPlayers (Game game)
    {
        MsoyMatchConfig match = null;
        try {
            if (game != null && !StringUtil.isBlank(game.config)) {
                match = (MsoyMatchConfig)new MsoyGameParser().parseGame(game).match;
            }
            if (match == null) {
                log.warning("Game missing match configuration [game=" + game + "].");
            }
        } catch (Exception e) {
            log.warning("Failed to parse XML game definition [id=" + game.gameId +
                    ", config=" + game.config + "]", e);
        }
        if (match != null) {
            return new int[] {
                match.minSeats,
                (match.getMatchType() == GameConfig.PARTY) ? Integer.MAX_VALUE : match.maxSeats
            };
        }
        return new int[] { 1, 2 }; // arbitrary defaults
    }

    // from WorldService
    public String serializePopularPlaces (WebIdent ident, final int n)
        throws ServiceException
    {
        final MemberRecord mrec = _mhelper.getAuthedUser(ident);
        final MemberName name = (mrec == null) ? null : mrec.getName();

        // if we're logged on, fetch our friends and groups of which we're members
        final List<FriendEntry> friends;
        final Set<GroupName> groups = Sets.newHashSet();
        if (mrec != null) {
            try {
                friends = _memberRepo.loadFriends(mrec.memberId, -1);
                for (GroupRecord gRec : _groupRepo.getFullMemberships(mrec.memberId)) {
                    groups.add(new GroupName(gRec.name, gRec.groupId));
                }
            } catch (PersistenceException e) {
                log.warning("Failed to load friends or groups", e);
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }
            // we add ourselves to our friends list so that we see where we are as well
            friends.add(new FriendEntry(name, true, null, ""));
        } else {
            friends = Lists.newArrayList();
        }

        // now proceed to the dobj thread to get runtime state
        return runDObjectAction("serializePopularPlaces[" + n + "]", new DOAction<String>() {
            public String run () throws Exception {
                JSONObject result = new JSONObject();
                addPopularChannels(name, groups, result);
                addPopularPlaces(mrec, name, friends, result);
                return URLEncoder.encode(result.toString(), "UTF-8");
            }
        });
    }

    // from WorldService
    public MyWhirledData getMyWhirled (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            MyWhirledData data = new MyWhirledData();
            data.whirledPopulation = _memberMan.getPPSnapshot().getPopulationCount();

            IntSet friendIds = _memberRepo.loadFriendIds(mrec.memberId);
            data.friendCount = friendIds.size();
            if (data.friendCount > 0) {
                data.friends = _mhelper.resolveMemberCards(friendIds, true, friendIds);
            }

            IntSet groupMemberships = new ArrayIntSet();
            for (GroupMembershipRecord gmr : _groupRepo.getMemberships(mrec.memberId)) {
                groupMemberships.add(gmr.groupId);
            }
            data.feed = loadFeed(mrec, groupMemberships, DEFAULT_FEED_DAYS);

            return data;

        } catch (PersistenceException pe) {
            log.warning("getMyWhirled failed [for=" + mrec.memberId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WorldService
    public void updateWhirledNews (WebIdent ident, final String newsHtml)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        if (!mrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        postDObjectAction(new Runnable() {
            public void run () {
                RuntimeConfig.server.setWhirledwideNewsHtml(newsHtml);
            }
        });
    }

    // from interface WorldService
    public List<WorldService.Room> loadMyRooms (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            List<WorldService.Room> rooms = Lists.newArrayList();
            for (SceneBookmarkEntry scene : _sceneRepo.getOwnedScenes(mrec.memberId)) {
                WorldService.Room room = new WorldService.Room();
                room.sceneId = scene.sceneId;
                room.name = scene.sceneName;
                // TODO: load decor thumbnail
                rooms.add(room);
            }
            return rooms;

        } catch (PersistenceException pe) {
            log.warning("Load rooms failed [memberId=" + mrec.memberId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WorldService
    public List<FeedMessage> loadFeed (WebIdent ident, int cutoffDays)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            List<GroupMembershipRecord> groups = _groupRepo.getMemberships(mrec.memberId);
            ArrayIntSet groupIds = new ArrayIntSet(groups.size());
            for (GroupMembershipRecord record : groups) {
                groupIds.add(record.groupId);
            }
            return loadFeed(mrec, groupIds, cutoffDays);

        } catch (PersistenceException pe) {
            log.warning("Load feed failed [memberId=" + mrec.memberId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WorldService
    public LaunchConfig loadLaunchConfig (WebIdent ident, int gameId)
        throws ServiceException
    {
        return _gameLogic.loadLaunchConfig(ident, gameId);
    }

    // from interface WorldService
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

    /**
     * Helper function for {@link #loadFeed} and {@link #getMyWhirled}.
     */
    protected List<FeedMessage> loadFeed (MemberRecord mrec, IntSet groupIds, int cutoffDays)
        throws PersistenceException
    {
        Timestamp since = new Timestamp(System.currentTimeMillis() - cutoffDays * 24*60*60*1000L);
        IntSet friendIds = _memberRepo.loadFriendIds(mrec.memberId);
        return _servletLogic.resolveFeedMessages(
            _feedRepo.loadPersonalFeed(mrec.memberId, friendIds, groupIds, since));
    }

    /**
     * Adds popular chat channel information to the supplied "My Whirled" result.
     */
    @EventThread
    protected void addPopularChannels (MemberName name, Set<GroupName> groups, JSONObject result)
        throws JSONException
    {
        JSONArray channels = new JSONArray();
        Iterable<ChatChannel> allChannels = _channelMan.getChatChannels();
        int desiredChannels = 8;

        // first add active channels we're members of
        for (ChatChannel channel : allChannels) {
            if (--desiredChannels < 0) {
                break;
            }
            if (channel.type == ChatChannel.GROUP_CHANNEL &&
                groups.contains(channel.ident)) {
                JSONObject cObj = new JSONObject();
                cObj.put("name", ((GroupName) channel.ident).toString());
                cObj.put("id", ((GroupName) channel.ident).getGroupId());
                channels.put(cObj);
            }
        }
        // then fill in with the ones we're not members of, if needed
        for (ChatChannel channel : allChannels) {
            if (--desiredChannels < 0) {
                break;
            }
            if (channel.type == ChatChannel.GROUP_CHANNEL &&
                !groups.contains(channel.ident)) {
                JSONObject cObj = new JSONObject();
                cObj.put("name", ((GroupName) channel.ident).toString());
                cObj.put("id", ((GroupName) channel.ident).getGroupId());
                channels.put(cObj);
            }
        }

        result.put("channels", channels);
    }

    /**
     * Adds popular places (scenes and games) information to the supplied "My Whirled" result.
     */
    @EventThread
    protected void addPopularPlaces (MemberRecord mrec, MemberName name,
                                     final List<FriendEntry> friends, JSONObject result)
        throws JSONException
    {
        final IntMap<PlaceDetail> scenes = IntMaps.newHashIntMap();
        final IntMap<PlaceDetail> games = IntMaps.newHashIntMap();
        final PopularPlacesSnapshot snap = _memberMan.getPPSnapshot();

        // locate all of our online friends
        _peerMan.applyToNodes(new PeerManager.Operation() {
            public void apply (NodeObject nodeobj) {
                MsoyNodeObject mnobj = (MsoyNodeObject)nodeobj;

                // see which (if any) of our friends are on this server
                for (FriendEntry entry : friends) {
                    MemberLocation memLoc = mnobj.memberLocs.get(entry.name.getMemberId());
                    if (memLoc == null) {
                        continue;
                    }
                    if (memLoc.sceneId != 0) {
                        noteFriend(scenes, entry, snap.getScene(memLoc.sceneId));
                    }
                    if (memLoc.gameId != 0 && !Game.isDeveloperVersion(memLoc.gameId) &&
                        memLoc.gameId != Game.TUTORIAL_GAME_ID) {
                        noteFriend(games, entry, snap.getGame(memLoc.gameId));
                    }
                }
            }

            protected void noteFriend (IntMap<PlaceDetail> dplaces, FriendEntry entry,
                                       PlaceCard place) {
                if (place == null) {
                    return;
                }
                PlaceDetail dplace = dplaces.get(place.placeId);
                if (dplace == null) {
                    dplaces.put(place.placeId, dplace = new PlaceDetail());
                    dplace.place = place;
                }
                dplace.friends.add(entry.name);
            }
        });

        // after we've located our friends, we add in the top populous places too
        addTopPopularPlaces(snap.getTopScenes(), scenes);
        addTopPopularPlaces(snap.getTopGames(), games);

        // if we're logged in we want to note our home as the central spot (and remove it from the
        // list of normal scenes)
        PlaceDetail home = null;
        if (mrec != null) {
            home = scenes.remove(mrec.homeSceneId);
            if (home == null) {
                home = new PlaceDetail();
                home.place = snap.getScene(mrec.homeSceneId);
                if (home.place == null) {
                    home.place = new PlaceCard();
                    home.place.placeId = mrec.homeSceneId;
                }
            }
        }

        // now convert all these places into JSON bits
        JSONArray jscenes = new JSONArray();
        for (PlaceDetail dplace : scenes.values()) {
            jscenes.put(placeToJSON(name, dplace));
        }
        JSONArray jgames = new JSONArray();
        for (PlaceDetail dplace : games.values()) {
            jgames.put(placeToJSON(name, dplace));
        }
//         JSONArray groups = new JSONArray();

        if (home != null) {
            result.put("member", placeToJSON(name, home));
        }
        result.put("scenes", jscenes);
//         result.put("groups", groups);
        result.put("games", jgames);
        result.put("totpop", snap.getPopulationCount());
    }

    @EventThread
    protected void addTopPopularPlaces (Iterable<PlaceCard> top, IntMap<PlaceDetail> map)
    {
        int n = 3; // TODO: totally ad-hoc
        for (PlaceCard place : top) {
            if (map.containsKey(place.placeId)) {
                continue;
            }
            PlaceDetail dplace = new PlaceDetail();
            dplace.place = place;
            map.put(place.placeId, dplace);
            if (--n <= 0) {
                return;
            }
        }
    }

    protected JSONObject placeToJSON (MemberName who, PlaceDetail dplace)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put("placeId", dplace.place.placeId);
        obj.put("name", dplace.place.name);
        obj.put("pcount", dplace.place.population);
        if (dplace.friends.size() > 0) {
            JSONArray arr = new JSONArray();
            for (MemberName name : dplace.friends) {
                arr.put(name.equals(who) ? "You" : name.toString()); // TODO: localize "You"
            }
            obj.put("friends", arr);
        }
        return obj;
    }

    protected static class PlaceDetail
    {
        public PlaceCard place;
        public List<MemberName> friends = Lists.newArrayList();
    }

    /** Contains a cached copy of our WhatIsWhirled data. */
    protected ExpiringReference<LandingData> _landingData;

    // our dependencies
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MemberManager _memberMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected ChatChannelManager _channelMan;
    @Inject protected ServletLogic _servletLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected MsoySceneRepository _sceneRepo;

    protected static final int TARGET_MYWHIRLED_GAMES = 6;
    protected static final int DEFAULT_FEED_DAYS = 2;

    protected static final long LANDING_DATA_EXPIRY = /* 60*60* */ 1000L;
}
