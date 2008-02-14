//
// $Id$

package com.threerings.msoy.web.server;

import java.net.URLEncoder;
import java.sql.Timestamp;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ExpiringReference;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.rating.server.persist.RatingRecord;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.xml.MsoyGameParser;

import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.server.persist.SceneRecord;

import com.threerings.msoy.person.data.FeedMessage;
import com.threerings.msoy.person.data.FriendFeedMessage;
import com.threerings.msoy.person.data.GroupFeedMessage;
import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.person.server.persist.FeedMessageRecord;
import com.threerings.msoy.person.server.persist.FriendFeedMessageRecord;
import com.threerings.msoy.person.server.persist.GroupFeedMessageRecord;
import com.threerings.msoy.person.server.persist.ProfileRecord;

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
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.WorldService;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MyWhirledData;
import com.threerings.msoy.web.data.SceneCard;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.WhatIsWhirledData;
import com.threerings.msoy.web.data.WhirledwideData;

import static com.threerings.msoy.Log.log;

/**
 * Does something extraordinary.
 */
public class WorldServlet extends MsoyServiceServlet
    implements WorldService
{
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

        final IntSet friendIds;
        ProfileRecord profile;
        Map<Integer, String> ownedRooms = Maps.newHashMap();
        final IntSet groupMemberships = new ArrayIntSet();
        List<FeedMessage> feed;

        try {
            friendIds = MsoyServer.memberRepo.loadFriendIds(mrec.memberId);
            profile = MsoyServer.profileRepo.loadProfile(mrec.memberId);
            for (SceneBookmarkEntry scene : MsoyServer.sceneRepo.getOwnedScenes(mrec.memberId)) {
                ownedRooms.put(scene.sceneId, scene.sceneName);
            }
            for (GroupMembershipRecord gmr : MsoyServer.groupRepo.getMemberships(mrec.memberId)) {
                groupMemberships.add(gmr.groupId);
            }
            // load up our feed information before we start fiddling with groupMemberships
            feed = loadFeed(mrec, groupMemberships, DEFAULT_FEED_DAYS);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getMyWhirled failed [for=" + mrec.memberId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        MyWhirledData data = new MyWhirledData();
        if (profile != null) {
            data.photo = (profile.photoHash == null) ? null : profile.getPhoto();
        }
        data.homeSceneId = mrec.homeSceneId;
        data.friends = ServletUtil.resolveMemberCards(friendIds, true);
        data.feed = feed;
        data.rooms = ownedRooms;
        return data;
    }

    // from WorldService
    public WhirledwideData getWhirledwide ()
        throws ServiceException
    {
        final WhirledwideData whirledwide = new WhirledwideData();

        // get the top 9 rated Game SceneCards.  We sort them by rating here on the server, and
        // avoid fill in info that is unneeded, like population
        try {
            List<SceneCard> games = Lists.newArrayList();
            // fetch catalog records and loop over them
            for (CatalogRecord record : MsoyServer.itemMan.getGameRepository().loadCatalog(
                    CatalogListing.SORT_BY_RATING, false, null, 0, 0, 0, 9)) {
                GameRecord gameRec = (GameRecord) record.item;
                SceneCard game = new SceneCard();
                game.sceneId = gameRec.gameId;
                game.name = gameRec.name;
                game.logo = gameRec.thumbMediaHash == null ? null :
                    new MediaDesc(gameRec.thumbMediaHash, gameRec.thumbMimeType,
                                  gameRec.thumbConstraint);
                game.sceneType = SceneCard.GAME;
                games.add(game);
            }
            whirledwide.games = games;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to get popular games info", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        List<MemberCard> whirledPeople = Lists.newArrayList();
        final List<MemberCard> people = Lists.newArrayList();
        ServletUtil.invokePeerOperation("getWhirledwide", new PeerManager.Operation() {
            public void apply (NodeObject nodeobj) {
                MsoyNodeObject mnobj = (MsoyNodeObject) nodeobj;
                for (MemberLocation memberLoc : mnobj.memberLocs) {
                    if (memberLoc.memberId == MemberName.GUEST_ID) {
                        // don't include guests.
                        continue;
                    }
                    MemberCard member = new MemberCard();
                    // card details get filled in back on the servlet thread
                    member.name = new MemberName("", memberLoc.memberId);
                    people.add(member);
                }
            }
        });
        for (int ii = 0; ii < 5 && people.size() > 0; ii++) {
            int randomPerson = (int) (Math.random() * people.size());
            whirledPeople.add(people.remove(randomPerson));
        }

        // Member cards
        try {
            for (MemberCard card : whirledPeople) {
                MemberNameRecord name =
                    MsoyServer.memberRepo.loadMemberName(card.name.getMemberId());
                card.name = name.toMemberName();
                ProfileRecord profile =
                    MsoyServer.profileRepo.loadProfile(card.name.getMemberId());
                if (profile == null) {
                    log.warning("Missing profile for card [who=" + card.name + "].");
                } else if (profile.photoHash != null) {
                    card.photo = new MediaDesc(profile.photoHash, profile.photoMimeType,
                                               profile.photoConstraint);
                }
            }
            whirledwide.people = whirledPeople;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to flesh out MemberCards", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        // Scene cards
        PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();
        whirledwide.whirledPopulation = pps.getPopulationCount();
        List<SceneCard> cards = Lists.newArrayList();
        for (PopularPlacesSnapshot.Place scene : pps.getTopScenes()) {
            SceneCard card = new SceneCard();
            card.sceneType = SceneCard.ROOM;
            card.sceneId = scene.placeId;
            card.name = scene.name;
            card.population = scene.population;
            cards.add(card);
        }
        whirledwide.places = cards;

        whirledwide.newsHtml = RuntimeConfig.server.whirledwideNewsHtml;
        return whirledwide;
    }

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
        MemberRecord mrec = getAuthedUser(ident);

        // load up the metadata for this game
        GameRepository repo = MsoyServer.itemMan.getGameRepository();
        GameRecord grec;
        try {
            grec = repo.loadGameRecord(gameId);
            if (grec == null) {
                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load game record [gameId=" + gameId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
        Game game = (Game)grec.toItem();

        // create a launch config record for the game
        LaunchConfig config = new LaunchConfig();
        config.gameId = game.gameId;

        MsoyMatchConfig match;
        try {
            if (StringUtil.isBlank(game.config)) {
                // fall back to a sensible default for our legacy games
                match = new MsoyMatchConfig();
                match.minSeats = match.startSeats = 1;
                match.maxSeats = 2;
            } else {
                MsoyGameDefinition def = (MsoyGameDefinition)new MsoyGameParser().parseGame(game);
                config.lwjgl = def.lwjgl;
                match = (MsoyMatchConfig)def.match;
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to parse XML game definition [id=" + gameId + "]", e);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }

        switch (game.gameMedia.mimeType) {
        case MediaDesc.APPLICATION_SHOCKWAVE_FLASH:
            config.type = game.isInWorld() ?
                    LaunchConfig.FLASH_IN_WORLD : LaunchConfig.FLASH_LOBBIED;
            break;
        case MediaDesc.APPLICATION_JAVA_ARCHIVE:
            // ignore maxSeats in the case of a party game - always display a lobby
            config.type = (!match.isPartyGame && match.maxSeats == 1) ?
                LaunchConfig.JAVA_SOLO : LaunchConfig.JAVA_FLASH_LOBBIED;
            break;
        default:
            log.warning("Requested config for game of unknown media type " +
                        "[id=" + gameId + ", media=" + game.gameMedia + "].");
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        // we have to proxy game jar files through the game server due to the applet sandbox
        config.gameMediaPath = (game.gameMedia.mimeType == MediaDesc.APPLICATION_JAVA_ARCHIVE) ?
            game.gameMedia.getProxyMediaPath() : game.gameMedia.getMediaPath();
        config.name = game.name;
        config.httpPort = ServerConfig.httpPort;

        // determine what server is hosting the game, start hosting it if necessary
        final GameLocationWaiter waiter = new GameLocationWaiter(config.gameId);
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    MsoyServer.gameReg.locateGame(null, waiter.gameId, waiter);
                } catch (InvocationException ie) {
                    waiter.requestFailed(ie);
                }
            }
        });
        Tuple<String, Integer> rhost = waiter.waitForResult();
        config.server = rhost.left;
        config.port = rhost.right;

        return config;
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
            for (MemberNameRecord name : MsoyServer.memberRepo.loadMemberNames(feedFriendIds)) {
                memberLookup.put(name.memberId, name.toMemberName());
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
     * fills an array list of SceneCards, using the map to fill up the SceneCard's friends list.
     */
    protected List<SceneCard> getRoomSceneCards (IntMap<List<Integer>> map,
                                                 PopularPlacesSnapshot pps)
        throws ServiceException
    {
        IntMap<SceneCard> cards = IntMaps.newHashIntMap();
        List<SceneCard> returnCards = Lists.newArrayList();

        try {
            // maps group id to the scene(s) that are owned by it
            IntMap<IntSet> groupIds = IntMaps.newHashIntMap();
            // maps member id to the scene(s) that are owned by them
            IntMap<IntSet> memIds = IntMaps.newHashIntMap();
            for (SceneRecord sceneRec : MsoyServer.sceneRepo.loadScenes(map.keySet())) {
                SceneCard card = new SceneCard();
                card.sceneId = sceneRec.sceneId;
                card.name = sceneRec.name;
                card.sceneType = SceneCard.ROOM;
                card.friends = map.get(card.sceneId);
                PopularPlacesSnapshot.Place snap = pps.getScene(sceneRec.sceneId);
                // if the snapshot is out of date, the display will be made sane in GWT.
                card.population = snap == null ? 0 : snap.population;
                cards.put(card.sceneId, card);
                if (sceneRec.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                    IntSet groupScenes = groupIds.get(sceneRec.ownerId);
                    if (groupScenes == null) {
                        groupScenes = new ArrayIntSet();
                        groupIds.put(sceneRec.ownerId, groupScenes);
                    }
                    groupScenes.add(sceneRec.sceneId);
                } else if (sceneRec.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                    IntSet memberScenes = memIds.get(sceneRec.ownerId);
                    if (memberScenes == null) {
                        memberScenes = new ArrayIntSet();
                        memIds.put(sceneRec.ownerId, memberScenes);
                    }
                    memberScenes.add(sceneRec.sceneId);
                }
            }

            // fill in logos for group-owned scenes
            for (GroupRecord groupRec : MsoyServer.groupRepo.loadGroups(groupIds.keySet())) {
                for (int sceneId : groupIds.remove(groupRec.groupId)) {
                    SceneCard card = cards.remove(sceneId);
                    if (card != null) {
                        card.logo = groupRec.logoMediaHash == null ?
                            Group.getDefaultGroupLogoMedia() :
                            new MediaDesc(groupRec.logoMediaHash, groupRec.logoMimeType,
                                          groupRec.logoMediaConstraint);
                        returnCards.add(card);
                    }
                }
            }

            // fill in default logos for scenes whose group has been removed
            for (int groupId : groupIds.keySet()) {
                for (int sceneId : groupIds.remove(groupId)) {
                    SceneCard card = cards.remove(sceneId);
                    if (card != null) {
                        card.logo = Group.getDefaultGroupLogoMedia();
                        returnCards.add(card);
                    }
                }
            }

            // fill in logos for member-owned scenes
            for (ProfileRecord profileRec : MsoyServer.profileRepo.loadProfiles(memIds.keySet())) {
                for (int sceneId : memIds.remove(profileRec.memberId)) {
                    SceneCard card = cards.remove(sceneId);
                    if (card != null) {
                        card.logo = profileRec.photoHash == null ? Profile.DEFAULT_PHOTO :
                            new MediaDesc(profileRec.photoHash, profileRec.photoMimeType,
                                        profileRec.photoConstraint);
                        returnCards.add(card);
                    }
                }
            }

            // fill in default logo for scenes whose owner has no profile
            for (int memberId : memIds.keySet()) {
                for (int sceneId : memIds.remove(memberId)) {
                    SceneCard card = cards.remove(sceneId);
                    if (card != null) {
                        card.logo = Profile.DEFAULT_PHOTO;
                        returnCards.add(card);
                    }
                }
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "failed to fill in SceneCards for rooms...", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        return returnCards;
    }

    /**
     * Fills an array list of SceneCards, using the map to fill up the SceneCard's friends list.
     */
    protected List<SceneCard> getGameSceneCards (IntMap<List<Integer>> map,
                                                 PopularPlacesSnapshot pps)
        throws ServiceException
    {
        List<SceneCard> cards = Lists.newArrayList();

        try {
            for (int gameId : map.intKeySet()) {
                GameRecord gameRec =
                    MsoyServer.itemMan.getGameRepository().loadGameRecord(gameId);
                if (gameRec == null) {
                    log.warning("Missing game record for game [id=" + gameId + "]");
                    continue;
                }
                SceneCard card = new SceneCard();
                card.sceneId = gameId;
                card.name = gameRec.name;
                card.sceneType = SceneCard.GAME;
                card.friends = map.get(gameId);
                card.logo = gameRec.thumbMediaHash == null ? null :
                    new MediaDesc(gameRec.thumbMediaHash, gameRec.thumbMimeType,
                                  gameRec.thumbConstraint);
                PopularPlacesSnapshot.Place snap = pps.getGame(gameId);
                // if the snapshot is out of date, the display will be made sane in GWT.
                card.population = (snap == null) ? 0 : snap.population;
                cards.add(card);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "failed to fill in SceneCards for games...", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        return cards;
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
                                       PopularPlacesSnapshot.Place place) {
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
                    home.place = new PopularPlacesSnapshot.Place();
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

    protected void addTopPopularPlaces (Iterable<PopularPlacesSnapshot.Place> top,
                                        IntMap<PlaceDetail> map)
    {
        int n = 3; // TODO: totally ad-hoc
        for (PopularPlacesSnapshot.Place place : top) {
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
        public PopularPlacesSnapshot.Place place;
        public List<MemberName> friends = Lists.newArrayList();
    }

    protected static class GameLocationWaiter extends ServletWaiter<Tuple<String,Integer>>
        implements MsoyGameService.LocationListener
    {
        public int gameId;
        public GameLocationWaiter (int gameId) {
            super("locateGame(" + gameId + ")");
            this.gameId = gameId;
        }
        public void gameLocated (String host, int port) {
            postSuccess(new Tuple<String,Integer>(host, port));
        }
        public void requestFailed (String cause) {
            requestFailed(new InvocationException(cause));
        }
    }

    /** Contains a cached copy of our WhatIsWhirled data. */
    protected ExpiringReference<WhatIsWhirledData> _whatIsWhirled;

    protected static final int TARGET_MYWHIRLED_GAMES = 6;
    protected static final int DEFAULT_FEED_DAYS = 2;

    protected static final long WHAT_IS_WHIRLED_EXPIRY = /* 60*60* */ 1000L;
}
