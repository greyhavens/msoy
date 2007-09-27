//
// $Id$

package com.threerings.msoy.web.server;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntSet;

import org.apache.velocity.VelocityContext;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.world.server.persist.SceneRecord;

import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.MyWhirledData;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.SceneCard;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.WhirledwideData;
import com.threerings.msoy.world.data.MsoySceneModel;

import com.threerings.msoy.data.MsoyAuthCodes;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link MemberService}.
 */
public class MemberServlet extends MsoyServiceServlet
    implements MemberService
{
    // from MemberService
    public boolean getFriendStatus (WebIdent ident, final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        try {
            return MsoyServer.memberRepo.getFriendStatus(memrec.memberId, memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getFriendStatus failed [for=" + memberId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void addFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        try {
            final MemberName friendName =
                MsoyServer.memberRepo.noteFriendship(memrec.memberId, friendId);
            if (friendName == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.friendMan.friendshipEstablished(memrec.getName(), friendName);
                }
            });

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "addFriend failed [for=" + memrec.memberId +
                    ", friendId=" + friendId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void removeFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        try {
            MsoyServer.memberRepo.clearFriendship(memrec.memberId, friendId);
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.friendMan.friendshipCleared(memrec.memberId, friendId);
                }
            });

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "removeFriend failed [for=" + memrec.memberId +
                    ", friendId=" + friendId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface MemberService
    public List loadInventory (WebIdent ident, byte type, int suiteId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        // convert the string they supplied to an item enumeration
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + ident + ", type=" + type + "].");
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
        try {
            ArrayList<Item> items = new ArrayList<Item>();
            for (ItemRecord record : repo.loadOriginalItems(memrec.memberId, suiteId)) {
                items.add(record.toItem());
            }
            for (ItemRecord record : repo.loadClonedItems(memrec.memberId, suiteId)) {
                items.add(record.toItem());
            }

            // when Item becomes a type-safe Comparable this Comparator can go away
            Collections.sort(items, new Comparator<Item>() {
                public int compare (Item one, Item two) {
                    return one.compareTo(two);
                }
            });
            return items;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "loadInventory failed [for=" + memrec.memberId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public String serializePopularPlaces (WebIdent ident, final int n)
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser(ident);
        final MemberName name = (mrec == null) ? null : mrec.getName();

        // if we're logged on, fetch our friends and groups of which we're members
        final List<FriendEntry> friends;
        final Set<GroupName> groups = new HashSet<GroupName>();
        if (mrec != null) {
            try {
                friends = MsoyServer.memberRepo.loadFriends(mrec.memberId);
                for (GroupRecord gRec : MsoyServer.groupRepo.getFullMemberships(mrec.memberId)) {
                    groups.add(new GroupName(gRec.name, gRec.groupId));
                }
            } catch (PersistenceException e) {
                log.log(Level.WARNING, "Failed to load friends or groups", e);
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }
            // we add ourselves to our friends list so that we see where we are as well
            friends.add(new FriendEntry(name, true));
        } else {
            friends = new ArrayList<FriendEntry>();
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

    // from MemberService 
    public MyWhirledData getMyWhirled (WebIdent ident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        final List<FriendEntry> friends;
        ProfileRecord profile;
        HashMap<Integer, String> ownedRooms = new HashMap<Integer, String>();
        final ArrayIntSet groupMemberships = new ArrayIntSet();

        try {
            friends = MsoyServer.memberRepo.loadFriends(memrec.memberId);
            profile = MsoyServer.profileRepo.loadProfile(memrec.memberId);
            for (SceneBookmarkEntry scene : MsoyServer.sceneRepo.getOwnedScenes(memrec.memberId)) {
                ownedRooms.put(scene.sceneId, scene.sceneName);
            }
            for (GroupMembershipRecord groupMembershipRec : 
                    MsoyServer.groupRepo.getMemberships(memrec.memberId)) {
                groupMemberships.add(groupMembershipRec.groupId);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Fetching friend list, profile, or room list failed! " +
                "[memberId=" + memrec.memberId + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // filter out who's not online on the dobj thread
        final HashIntMap<MemberCard> onlineFriends = new HashIntMap<MemberCard>();
        final HashIntMap<ArrayList<Integer>> places = new HashIntMap<ArrayList<Integer>>();
        final HashIntMap<ArrayList<Integer>> games = new HashIntMap<ArrayList<Integer>>();
        final HashMap<Integer, String> chats = new HashMap<Integer, String>();
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "getMyWhirled [memberId=" + memrec.memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    MsoyServer.peerMan.applyToNodes(new PeerManager.Operation() {
                        public void apply (NodeObject nodeobj) {
                            MsoyNodeObject mnobj = (MsoyNodeObject)nodeobj;
                            for (FriendEntry friend : friends) {
                                MemberLocation memLoc = 
                                    mnobj.memberLocs.get(friend.name.getMemberId());
                                if (memLoc == null) {
                                    continue;
                                }

                                MemberCard memberCard = new MemberCard();
                                memberCard.name = friend.name;
                                onlineFriends.put(memberCard.name.getMemberId(), memberCard);
                                if (memLoc.sceneId == 0 && memLoc.gameId == 0) {
                                    continue;
                                }

                                if (memLoc.sceneId != 0) {
                                    ArrayList<Integer> list = places.get(memLoc.sceneId);
                                    if (list == null) {
                                        list = new ArrayList<Integer>();
                                        places.put(memLoc.sceneId, list);
                                    }
                                    list.add(memberCard.name.getMemberId());
                                } 

                                if (memLoc.gameId != 0) {
                                    ArrayList<Integer> list = games.get(memLoc.gameId);
                                    if (list == null) {
                                        list = new ArrayList<Integer>();
                                        games.put(memLoc.gameId, list);
                                    }
                                    list.add(memberCard.name.getMemberId());
                                }
                            }

                            // for now, we're going to list all active games...
                            for (HostedGame game : mnobj.hostedGames) {
                                if (games.get(game.placeId) == null) {
                                    games.put(game.placeId, new ArrayList<Integer>());
                                }
                            }

                            // check if any of our groups have a chat hosted here...
                            for (HostedChannel chat : mnobj.hostedChannels) {
                                if (chat.channel.type == ChatChannel.GROUP_CHANNEL) {
                                    GroupName group = (GroupName) chat.channel.ident;
                                    if (groupMemberships.contains(group.getGroupId())) {
                                        chats.put(group.getGroupId(), "" + group);
                                        groupMemberships.remove(group.getGroupId());
                                    }
                                }
                            }
                        }
                    });
                    waiter.requestCompleted(null);
                } catch (Exception e) {
                    waiter.requestFailed(e);
                    return;
                }
            }
        });
        waiter.waitForResult();

        // flesh out profile data for the online friends
        try {
            for (ProfileRecord friendProfile : MsoyServer.profileRepo.loadProfiles(
                    onlineFriends.intKeySet().toIntArray())) {
                MemberCard card = onlineFriends.get(friendProfile.memberId);
                card.photo = friendProfile.getPhoto();
                card.headline = friendProfile.headline;
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to fill member cards", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();

        MyWhirledData myWhirled = new MyWhirledData();
        myWhirled.places = getRoomSceneCards(places, pps);
        myWhirled.games = getGameSceneCards(games, pps);
        myWhirled.people = new ArrayList<MemberCard>(onlineFriends.values());
        myWhirled.photo = profile.photoHash == null ? null : profile.getPhoto();
        myWhirled.ownedRooms = ownedRooms;
        myWhirled.chats = chats;
        myWhirled.whirledPopulation = pps.getPopulationCount();
        return myWhirled;
    }

    // from MemberService
    public WhirledwideData getWhirledwide ()
        throws ServiceException
    {
        final WhirledwideData whirledwide = new WhirledwideData();

        // get the top 9 rated Game SceneCards.  We sort them by rating here on the server, and 
        // avoid fill in info that is unneeded, like population
        try {
            ArrayList<SceneCard> games = new ArrayList<SceneCard>();
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
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        final ArrayList<MemberCard> whirledPeople = new ArrayList<MemberCard>();
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>("getWhirledwide");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    // TODO: do this in the popular places snapshot
                    final ArrayList<MemberCard> people = new ArrayList<MemberCard>();
                    MsoyServer.peerMan.applyToNodes(new PeerManager.Operation() {
                        public void apply (NodeObject nodeobj) {
                            MsoyNodeObject mnobj = (MsoyNodeObject) nodeobj;
                            for (MemberLocation memberLoc : mnobj.memberLocs) {
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
                    waiter.requestCompleted(null);
                } catch (Exception e) {
                    waiter.requestFailed(e);
                    return;
                }
            }
        });
        waiter.waitForResult();

        // Member cards
        try {
            for (MemberCard card : whirledPeople) {
                MemberNameRecord name =
                    MsoyServer.memberRepo.loadMemberName(card.name.getMemberId());
                if (name == null) {
                    log.warning("Retrieved null MemberNameRecord for MemberCard member [" + 
                        card.name.getMemberId() + "]");
                    continue;
                }
                card.name = name.toMemberName();
                ProfileRecord profile = 
                    MsoyServer.profileRepo.loadProfile(card.name.getMemberId());
                if (profile.photoHash != null) {
                    card.photo = new MediaDesc(profile.photoHash, profile.photoMimeType,
                                               profile.photoConstraint);
                }
            }
            whirledwide.people = whirledPeople;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to flesh out MemberCards", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // Scene cards
        PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();
        whirledwide.whirledPopulation = pps.getPopulationCount();
        ArrayList<SceneCard> cards = new ArrayList<SceneCard>();
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

    // from MemberService
    public MemberInvites getInvitationsStatus (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            MemberInvites result = new MemberInvites();
            result.availableInvitations = MsoyServer.memberRepo.getInvitesGranted(mrec.memberId);
            ArrayList<Invitation> pending = new ArrayList<Invitation>();
            for (InvitationRecord iRec : MsoyServer.memberRepo.loadPendingInvites(mrec.memberId)) {
                // we issued these invites so we are the inviter
                pending.add(iRec.toInvitation(mrec.getName()));
            }
            result.pendingInvitations = pending;
            result.serverUrl = ServerConfig.getServerURL() + "/#invite-";
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitationsStatus failed [id=" + mrec.memberId +"]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public InvitationResults sendInvites (WebIdent ident, List addresses, String customMessage)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            // make sure this user still has available invites; we already check this value in GWT
            // land, and deal with it sensibly there
            if (MsoyServer.memberRepo.getInvitesGranted(mrec.memberId) < addresses.size()) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitesGranted failed [id=" + mrec.memberId +"]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        InvitationResults ir = new InvitationResults();
        ir.results = new String[addresses.size()];
        for (int ii = 0; ii < addresses.size(); ii++) {
            String email = (String)addresses.get(ii);
            ir.results[ii] = sendInvite(mrec, email, customMessage);
        }
        return ir;
    }

    // from MemberService
    public Invitation getInvitation (String inviteId, boolean viewing)
        throws ServiceException
    {
        try {
            InvitationRecord invRec = MsoyServer.memberRepo.loadInvite(inviteId, viewing);
            if (invRec == null) {
                return null;
            }
            MemberNameRecord mnr = MsoyServer.memberRepo.loadMemberName(invRec.inviterId);
            return invRec.toInvitation(mnr.toMemberName());

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitation failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void optOut (Invitation invite)
        throws ServiceException
    {
        try {
            if (!MsoyServer.memberRepo.inviteAvailable(invite.inviteId)) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR);
            }
            MsoyServer.memberRepo.optOutInvite(invite);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "optOut failed [inviteId=" + invite.inviteId + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    /**
     * fills an array list of SceneCards, using the map to fill up the SceneCard's friends list.
     */
    protected ArrayList<SceneCard> getRoomSceneCards (HashIntMap<ArrayList<Integer>> map,
                                                      PopularPlacesSnapshot pps)
        throws ServiceException
    {
        HashIntMap<SceneCard> cards = new HashIntMap<SceneCard>();

        try {
            // maps group id to the scene(s) that are owned by it
            IntMap<IntSet> groupIds = new HashIntMap<IntSet>();
            // maps member id to the scene(s) that are owned by them
            IntMap<IntSet> memberIds = new HashIntMap<IntSet>();
            for (SceneRecord sceneRec : 
                    MsoyServer.sceneRepo.loadScenes(map.intKeySet().toIntArray())) {
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
                    IntSet memberScenes = memberIds.get(sceneRec.ownerId);
                    if (memberScenes == null) {
                        memberScenes = new ArrayIntSet();
                        memberIds.put(sceneRec.ownerId, memberScenes);
                    }
                    memberScenes.add(sceneRec.sceneId);
                }
            }

            // fill in logos for group-owned scenes
            for (GroupRecord groupRec : 
                    MsoyServer.groupRepo.loadGroups(groupIds.intKeySet().toIntArray())) {
                for (int sceneId : groupIds.get(groupRec.groupId)) {
                    SceneCard card = cards.get(sceneId);
                    if (card != null) {
                        card.logo = groupRec.logoMediaHash == null ? 
                            Group.getDefaultGroupLogoMedia() :
                            new MediaDesc(groupRec.logoMediaHash, groupRec.logoMimeType, 
                                          groupRec.logoMediaConstraint);
                    }
                }
            }

            // fill in logos for member-owned scenes
            for (ProfileRecord profileRec : 
                    MsoyServer.profileRepo.loadProfiles(memberIds.intKeySet().toIntArray())) {
                for (int sceneId : memberIds.get(profileRec.memberId)) {
                    SceneCard card = cards.get(sceneId);
                    if (card != null) {
                        card.logo = profileRec.photoHash == null ? Profile.DEFAULT_PHOTO : 
                            new MediaDesc(profileRec.photoHash, profileRec.photoMimeType, 
                                        profileRec.photoConstraint);
                    }
                }
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "failed to fill in SceneCards for rooms...", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    
        return new ArrayList<SceneCard>(cards.values());
    }

    /**
     * fills an array list of SceneCards, using the map to fill up the SceneCard's friends list.
     */
    protected ArrayList<SceneCard> getGameSceneCards (HashIntMap<ArrayList<Integer>> map,
                                                      PopularPlacesSnapshot pps)
        throws ServiceException
    {
        ArrayList<SceneCard> cards = new ArrayList<SceneCard>();

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
                card.population = snap == null ? 0 : snap.population;
                if (card.population > 0 || card.friends.size() > 0) {
                    cards.add(card);
                }
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "failed to fill in SceneCards for games...", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
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
        final HashIntMap<PlaceDetail> scenes = new HashIntMap<PlaceDetail>();
        final HashIntMap<PlaceDetail> games = new HashIntMap<PlaceDetail>();
        final PopularPlacesSnapshot snap = MsoyServer.memberMan.getPPSnapshot();

        // locate all of our online friends
        MsoyServer.peerMan.applyToNodes(new PeerManager.Operation() {
            public void apply (NodeObject nodeobj) {
                MsoyNodeObject mnobj = (MsoyNodeObject)nodeobj;

                // see which (if any) of our friends are on this server
                for (FriendEntry entry : friends) {
                    MemberLocation memloc = mnobj.memberLocs.get(entry.name.getMemberId());
                    if (memloc == null) {
                        continue;
                    }
                    if (memloc.sceneId > 0) {
                        noteFriend(scenes, entry, snap.getScene(memloc.sceneId));
                    }
                    if (memloc.gameId > 0) {
                        noteFriend(games, entry, snap.getGame(memloc.gameId));
                    }
                }
            }

            protected void noteFriend (HashIntMap<PlaceDetail> dplaces, FriendEntry entry,
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

    protected void addTopPopularPlaces (
        Iterable<PopularPlacesSnapshot.Place> top, HashIntMap<PlaceDetail> map)
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

    /**
     * Helper function for {@link #sendInvites}.
     */
    protected String sendInvite (MemberRecord inviter, String email, String customMessage)
    {
        try {
            // make sure this address is valid
            if (!MailUtil.isValidAddress(email)) {
                return InvitationResults.INVALID_EMAIL;
            }

            // make sure this address isn't already registered
            if (MsoyServer.memberRepo.loadMember(email) != null) {
                return InvitationResults.ALREADY_REGISTERED;
            }

            // make sure this address isn't on the opt-out list
            if (MsoyServer.memberRepo.hasOptedOut(email)) {
                return InvitationResults.OPTED_OUT;
            }

            // make sure this user hasn't already invited this address
            if (MsoyServer.memberRepo.loadInvite(email, inviter.memberId) != null) {
                return InvitationResults.ALREADY_INVITED;
            }

            String inviteId = MsoyServer.memberRepo.generateInviteId();

            // create and send the invitation
            VelocityContext ctx = new VelocityContext();
            ctx.put("friend", inviter.name);
            ctx.put("custom_message", customMessage);
            ctx.put("invite_id", inviteId);
            ctx.put("server_url", ServerConfig.getServerURL());

            try {
                MailSender.sendEmail(email, inviter.accountName, "memberInvite", ctx);
            } catch (Exception e) {
                return e.getMessage();
            }

            MsoyServer.memberRepo.addInvite(email, inviter.memberId, inviteId);
            return InvitationResults.SUCCESS;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "sendInvites failed.", pe);
            return ServiceException.INTERNAL_ERROR;
        }
    }

    protected static class PlaceDetail
    {
        public PopularPlacesSnapshot.Place place;
        public ArrayList<MemberName> friends = new ArrayList<MemberName>();
    }
}
