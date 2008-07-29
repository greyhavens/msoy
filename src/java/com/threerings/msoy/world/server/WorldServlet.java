//
// $Id$

package com.threerings.msoy.world.server;

import java.net.URLEncoder;

import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.server.ChatChannelManager;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.gwt.RoomInfo;
import com.threerings.msoy.world.gwt.WorldService;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;
import com.threerings.msoy.world.server.persist.SceneRecord;

import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Implements the {@link WorldService}.
 */
public class WorldServlet extends MsoyServiceServlet
    implements WorldService
{
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

    @EventThread
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

    // our dependencies
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MemberManager _memberMan;
    @Inject protected ChatChannelManager _channelMan;
    @Inject protected GameLogic _gameLogic;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GroupRepository _groupRepo;

    protected static final int TARGET_MYWHIRLED_GAMES = 6;
    protected static final int DEFAULT_FEED_DAYS = 2;
}
