//
// $Id$

package com.threerings.msoy.web.server;

import java.net.URLEncoder;
import java.sql.Timestamp;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ExpiringReference;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.rating.server.persist.RatingRecord;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.server.persist.SceneRecord;

import com.threerings.msoy.person.data.FeedMessage;
import com.threerings.msoy.person.data.FriendFeedMessage;
import com.threerings.msoy.person.data.GroupFeedMessage;
import com.threerings.msoy.person.server.persist.FeedMessageRecord;
import com.threerings.msoy.person.server.persist.FriendFeedMessageRecord;
import com.threerings.msoy.person.server.persist.GroupFeedMessageRecord;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.WorldService;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MyWhirledData;
import com.threerings.msoy.web.data.PlaceCard;
import com.threerings.msoy.web.data.RoomInfo;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.WhatIsWhirledData;

import static com.threerings.msoy.Log.log;

/**
 * Implements the {@link WorldService}.
 */
public class WorldServlet extends MsoyServiceServlet
    implements WorldService
{
    // from interface WorldService
    public WhatIsWhirledData getWhatIsWhirled ()
        throws ServiceException
    {
        try {
            WhatIsWhirledData data = ExpiringReference.get(_whatIsWhirled);
            if (data == null) {
                data = new WhatIsWhirledData();
                data.players = MsoyServer.memberRepo.getPopulationCount();
                data.places = MsoyServer.sceneRepo.getSceneCount();
                data.games = MsoyServer.itemMan.getGameRepository().getGameCount();
                _whatIsWhirled = ExpiringReference.create(data, WHAT_IS_WHIRLED_EXPIRY);
            }
            return data;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load WhatIsWhirled data.", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from WorldService
    public String serializePopularPlaces (WebIdent ident, final int n)
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser(ident);
        final MemberName name = (mrec == null) ? null : mrec.getName();

        // if we're logged on, fetch our friends and groups of which we're members
        final List<FriendEntry> friends;
        final Set<GroupName> groups = Sets.newHashSet();
        if (mrec != null) {
            try {
                friends = MsoyServer.memberRepo.loadFriends(mrec.memberId, -1);
                for (GroupRecord gRec : MsoyServer.groupRepo.getFullMemberships(mrec.memberId)) {
                    groups.add(new GroupName(gRec.name, gRec.groupId));
                }
            } catch (PersistenceException e) {
                log.log(Level.WARNING, "Failed to load friends or groups", e);
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }
            // we add ourselves to our friends list so that we see where we are as well
            friends.add(new FriendEntry(name, true, null));
        } else {
            friends = Lists.newArrayList();
        }

        // now proceed to the dobj thread to get runtime state
        final ServletWaiter<String> waiter = new ServletWaiter<String>(
            "serializePopularPlaces[" + n + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    JSONObject result = new JSONObject();
                    addPopularChannels(name, groups, result);
                    addPopularPlaces(mrec, name, friends, result);
                    waiter.requestCompleted(URLEncoder.encode(result.toString(), "UTF-8"));
                } catch (Exception e) {
                    waiter.requestFailed(e);
                    return;
                }
            }
        });
        return waiter.waitForResult();
    }

    // from WorldService
    public MyWhirledData getMyWhirled (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            MyWhirledData data = new MyWhirledData();
            data.whirledPopulation = MsoyServer.memberMan.getPPSnapshot().getPopulationCount();

            IntSet friendIds = MsoyServer.memberRepo.loadFriendIds(mrec.memberId);
            data.friendCount = friendIds.size();
            if (data.friendCount > 0) {
                data.friends = ServletUtil.resolveMemberCards(friendIds, true, friendIds);
            }

            IntSet groupMemberships = new ArrayIntSet();
            for (GroupMembershipRecord gmr : MsoyServer.groupRepo.getMemberships(mrec.memberId)) {
                groupMemberships.add(gmr.groupId);
            }
            data.feed = loadFeed(mrec, groupMemberships, DEFAULT_FEED_DAYS);

            return data;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getMyWhirled failed [for=" + mrec.memberId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WorldService
    public void updateWhirledNews (WebIdent ident, final String newsHtml)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);
        if (!mrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                RuntimeConfig.server.setWhirledwideNewsHtml(newsHtml);
            }
        });
    }

    // from interface WorldService
    public List loadMyRooms (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            List<WorldService.Room> rooms = Lists.newArrayList();
            for (SceneBookmarkEntry scene : MsoyServer.sceneRepo.getOwnedScenes(mrec.memberId)) {
                WorldService.Room room = new WorldService.Room();
                room.sceneId = scene.sceneId;
                room.name = scene.sceneName;
                // TODO: load decor thumbnail
                rooms.add(room);
            }
            return rooms;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Load rooms failed [memberId=" + mrec.memberId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WorldService
    public List loadFeed (WebIdent ident, int cutoffDays)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            List<GroupMembershipRecord> groups = MsoyServer.groupRepo.getMemberships(mrec.memberId);
            ArrayIntSet groupIds = new ArrayIntSet(groups.size());
            for (GroupMembershipRecord record : groups) {
                groupIds.add(record.groupId);
            }
            return loadFeed(mrec, groupIds, cutoffDays);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Load feed failed [memberId=" + mrec.memberId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WorldService
    public LaunchConfig loadLaunchConfig (WebIdent ident, int gameId)
        throws ServiceException
    {
        return ServletUtil.loadLaunchConfig(ident, gameId);
    }

    // from interface WorldService
    public RoomInfo loadRoomInfo (int sceneId)
        throws ServiceException
    {
        try {
            SceneRecord screc = MsoyServer.sceneRepo.loadScene(sceneId);
            if (screc == null) {
                return null;
            }

            RoomInfo info = new RoomInfo();
            info.sceneId = screc.sceneId;
            info.name = screc.name;
            switch (screc.ownerType) {
            case MsoySceneModel.OWNER_TYPE_MEMBER:
                info.owner = MsoyServer.memberRepo.loadMemberName(screc.ownerId);
                break;
            case MsoySceneModel.OWNER_TYPE_GROUP:
                info.owner = MsoyServer.groupRepo.loadGroupName(screc.ownerId);
                break;
            }
            return info;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Load room info failed [sceneId=" + sceneId + "]", pe);
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

        List<FeedMessage> messages = Lists.newArrayList();
        IntSet friendIds = MsoyServer.memberRepo.loadFriendIds(mrec.memberId);
        List<FeedMessageRecord> records = MsoyServer.feedRepo.loadMemberFeed(
            mrec.memberId, friendIds, groupIds, since);

        // find out which member and group names we'll need
        IntSet feedFriendIds = new ArrayIntSet(), feedGroupIds = new ArrayIntSet();
        for (FeedMessageRecord record : records) {
            if (record instanceof FriendFeedMessageRecord) {
                feedFriendIds.add(((FriendFeedMessageRecord)record).actorId);
            } else if (record instanceof GroupFeedMessageRecord) {
                feedGroupIds.add(((GroupFeedMessageRecord)record).groupId);
            }
        }

        // generate a lookup for the member names
        IntMap<MemberName> memberLookup = null;
        if (!feedFriendIds.isEmpty()) {
            memberLookup = IntMaps.newHashIntMap();
            for (MemberName name : MsoyServer.memberRepo.loadMemberNames(feedFriendIds)) {
                memberLookup.put(name.getMemberId(), name);
            }
        }

        // generate a lookup for the group names
        IntMap<GroupName> groupLookup = null;
        if (!feedGroupIds.isEmpty()) {
            groupLookup = IntMaps.newHashIntMap();
            for (GroupRecord group : MsoyServer.groupRepo.loadGroups(feedGroupIds)) {
                groupLookup.put(group.groupId, group.toGroupName());
            }
        }

        // create our list of feed messages
        for (FeedMessageRecord record : records) {
            FeedMessage message = record.toMessage();
            if (record instanceof FriendFeedMessageRecord) {
                ((FriendFeedMessage)message).friend =
                    memberLookup.get(((FriendFeedMessageRecord)record).actorId);
            } else if (record instanceof GroupFeedMessageRecord) {
                ((GroupFeedMessage)message).group =
                    groupLookup.get(((GroupFeedMessageRecord)record).groupId);
            }
            messages.add(message);
        }

        return messages;
    }

    /**
     * Adds popular chat channel information to the supplied "My Whirled" result.
     */
    protected void addPopularChannels (MemberName name, Set<GroupName> groups, JSONObject result)
        throws JSONException
    {
        JSONArray channels = new JSONArray();
        Iterable<ChatChannel> allChannels = MsoyServer.channelMan.getChatChannels();
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
    protected void addPopularPlaces (
        MemberRecord mrec, MemberName name, final List<FriendEntry> friends, JSONObject result)
        throws JSONException
    {
        final IntMap<PlaceDetail> scenes = IntMaps.newHashIntMap();
        final IntMap<PlaceDetail> games = IntMaps.newHashIntMap();
        final PopularPlacesSnapshot snap = MsoyServer.memberMan.getPPSnapshot();

        // locate all of our online friends
        MsoyServer.peerMan.applyToNodes(new PeerManager.Operation() {
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
    protected ExpiringReference<WhatIsWhirledData> _whatIsWhirled;

    protected static final int TARGET_MYWHIRLED_GAMES = 6;
    protected static final int DEFAULT_FEED_DAYS = 2;

    protected static final long WHAT_IS_WHIRLED_EXPIRY = /* 60*60* */ 1000L;
}
