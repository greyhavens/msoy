//
// $Id$

package com.threerings.msoy.server;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.samskivert.util.IntTuple;
import com.samskivert.util.ResultListener;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.SceneBookmarkEntry;
import com.threerings.msoy.game.data.LobbyConfig;
import com.threerings.msoy.game.server.LobbyManager;
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.FriendEntry;
import com.threerings.msoy.web.data.GroupName;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.NeighborGroup;
import com.threerings.msoy.web.data.NeighborMember;
import com.threerings.msoy.web.data.Neighborhood;
import com.threerings.msoy.web.data.PopularPlace.*;
import com.threerings.msoy.web.data.PopularPlace;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.server.RoomManager;

import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.GroupRepository;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.NeighborFriendRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manage msoy members.
 */
public class MemberManager
    implements MemberProvider
{
    /** Cache popular place computations for five seconds while we're debugging. */
    public static final long POPULAR_PLACES_CACHE_LIFE = 5*1000;

    /** The log for flow grants. */
    public static Logger flowLog = Logger.getLogger("com.threerings.msoy.flow");

    /**
     * Prepares our member manager for operation.
     */
    public void init (MemberRepository memberRepo, GroupRepository groupRepo)
    {
        _memberRepo = memberRepo;
        _groupRepo = groupRepo;
        MsoyServer.invmgr.registerDispatcher(new MemberDispatcher(this), MsoyCodes.BASE_GROUP);
    }

    /**
     * Loads the specified member's friends list.
     *
     * Note: all the friends will be marked as offline. If you desire to know their online status,
     * that should be filled in elsewhere.
     */
    public void loadFriends (final int memberId, ResultListener<List<FriendEntry>> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<FriendEntry>>(listener) {
            public List<FriendEntry> invokePersistResult ()
                throws PersistenceException {
                return _memberRepo.getFriends(memberId);
            }
        });
    }

    /**
     * Look up a member's name and construct a {@link MemberName} from it.
     */
    public void getName (final int memberId, ServletWaiter<MemberName> waiter)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<MemberName>(waiter) {
            public MemberName invokePersistResult () throws PersistenceException {
                MemberNameRecord record = _memberRepo.loadMemberName(memberId);
                if (record != null) {
                    return new MemberName(record.name, record.memberId);
                }
                return null;
            }
        });
    }

    /**
     * Look up some members' names and construct {@link MemberName}s from'em.
     */
    public void getNames (final int[] memberId, ServletWaiter<List<MemberName>> waiter)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<MemberName>>(waiter) {
            public List<MemberName> invokePersistResult () throws PersistenceException {
                List<MemberName> result = new ArrayList<MemberName>();
                for (MemberNameRecord record : _memberRepo.loadMemberNames(memberId)) {
                    result.add(new MemberName(record.name, record.memberId));
                }
                return result;
            }
        });
    }

    /**
     * Update the user's occupant info.
     */
    public void updateOccupantInfo (MemberObject user)
    {
        PlaceManager pmgr = MsoyServer.plreg.getPlaceManager(user.location);
        if (pmgr != null) {
            pmgr.updateOccupantInfo(user.createOccupantInfo(pmgr.getPlaceObject()));
        }
    }

    /**
     * Called when a member updates their display name. If they are online, we update their {@link
     * MemberObject} and all related occupant info records.
     */
    public void displayNameChanged (MemberName name)
    {
        MemberObject user = MsoyServer.lookupMember(name.getMemberId());
        if (user != null) {
            user.setMemberName(name);
            updateOccupantInfo(user);
        }
    }

    /**
     * Export alterFriend() functionality according to the web servlet way of doing things. 
     */
    public void alterFriend (int userId, int friendId, boolean add,
                             ResultListener<Void> listener)
    {
        MemberObject user = MsoyServer.lookupMember(userId);
        alterFriend(user, userId, friendId, add, listener);
    }

    /**
     * Fetch the home ID for a member and return it.
     */
    public void getHomeId (final byte ownerType, final int ownerId,
                           ResultListener<Integer> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Integer>(listener) {
            public Integer invokePersistResult () throws PersistenceException {
                switch (ownerType) {
                case MsoySceneModel.OWNER_TYPE_MEMBER:
                    MemberRecord member = _memberRepo.loadMember(ownerId);
                    return (member == null) ? null : member.homeSceneId;

                case MsoySceneModel.OWNER_TYPE_GROUP:
                    GroupRecord group = _groupRepo.loadGroup(ownerId);
                    return (group == null) ? null : group.homeSceneId;

                default:
                    log.warning("Unknown ownerType provided to getHomeId " +
                        "[ownerType=" + ownerType +
                        ", ownerId=" + ownerId + "].");
                    return null;
                }
            }
            public void handleSuccess () {
                if (_result == null) {
                    handleFailure(new InvocationException("m.no_such_user"));
                } else {
                    super.handleSuccess();
                }
            }
        });
    }
    
    /**
     * Return a JSON-serialized version of the Popular Places. This value is cached
     * for {@link POPULAR_PLACES_CACHE_LIFE} milliseconds.
     * TODO: The popular places code and the neighbourhood code are starting to share
     * increasing amounts of code, and some refactoring is due sometime soon.
     */
    public void serializePopularPlaces (int n, ResultListener<String> listener)
    {
        updatePPCache();

        try {
            JSONArray friends = new JSONArray();
            JSONArray groups = new JSONArray();
            JSONArray games = new JSONArray();
            for (PopularPlace place : _topPlaces) {
                JSONObject obj = new JSONObject();
                obj.put("name", place.name);
                obj.put("pop", place.population);
                if (place instanceof PopularGamePlace) {
                    obj.put("id", ((PopularGamePlace) place).gameId);
                    games.put(obj);
                } else {
                    obj.put("sceneId", ((PopularScenePlace) place).sceneId);
                    if (place instanceof PopularMemberPlace) {
                        obj.put("id", ((PopularMemberPlace) place).memberId);
                        friends.put(obj);
                    } else {
                        obj.put("id", ((PopularGroupPlace) place).groupId);
                        groups.put(obj);
                    }
                }
                if (--n <= 0) {
                    break;
                }
            }
            JSONObject result = new JSONObject();
            result.put("friends", friends);
            result.put("groups", groups);
            result.put("games", groups);
            listener.requestCompleted(URLEncoder.encode(result.toString(), "UTF-8"));
        } catch (Exception e) {
            listener.requestFailed(e);
        }
    }

    /**
     * Returns a summary of the scenes belonging to the given owner.
     */
    public PopularPlace getPopularPlace(int ownerType, int ownerId)
    {
        updatePPCache();
        return _scenesByOwner.get(new IntTuple(ownerType, ownerId));
    }

    /**
     * Constructs and returns the serialization of a {@link Neighborhood} record
     * for a given member or group.
     */
    public void serializeNeighborhood (final int id, final boolean forGroup,
                                       final ResultListener<String> listener)
    {
        ResultListener<Neighborhood> newListener = new ResultListener<Neighborhood>() {
            public void requestCompleted (Neighborhood result) {
                try {
                    listener.requestCompleted(
                        URLEncoder.encode(toJSON(result).toString(), "UTF-8"));
                } catch (Exception e) {
                    listener.requestFailed(e);
                }
            }
            public void requestFailed (Exception cause) {
                listener.requestFailed(cause);
            }
        };
        if (forGroup) {
            getGroupNeighborhood(id, newListener);
        } else {
            getMemberNeighborhood(id, newListener);
        }
    }

    /**
     * Constructs and returns a {@link Neighborhood} record for a given member.
     */
    public void getMemberNeighborhood (final int memberId, ResultListener<Neighborhood> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Neighborhood>(listener) {
            public Neighborhood invokePersistResult() throws PersistenceException {
                Neighborhood hood = new Neighborhood();
                // first load the center member data
                MemberRecord mRec = _memberRepo.loadMember(memberId);
                hood.member = makeNeighborMember(mRec);

                // then all the data for the groups
                Collection<GroupMembershipRecord> gmRecs = _groupRepo.getMemberships(memberId);
                int[] groupIds = new int[gmRecs.size()];
                int ii = 0;
                for (GroupMembershipRecord gmRec : gmRecs) {
                    groupIds[ii ++] = gmRec.groupId;
                }
                List<NeighborGroup> nGroups = new ArrayList<NeighborGroup>();
                for (GroupRecord gRec : _groupRepo.loadGroups(groupIds)) {
                    nGroups.add(makeNeighborGroup(gRec));
                }
                hood.neighborGroups = nGroups.toArray(new NeighborGroup[0]);

                // finally the friends
                List<NeighborMember> members = new ArrayList<NeighborMember>();
                for (NeighborFriendRecord fRec : _memberRepo.getNeighborhoodFriends(memberId)) {
                    members.add(makeNeighborMember(fRec));
                }
                hood.neighborMembers = members.toArray(new NeighborMember[0]);
                return hood;
            }

            // after we finish, have main thread go through
            public void handleSuccess () {
                // set online status and figure out populations
                finalizeNeighborhood(_result);
                _listener.requestCompleted(_result);
            }
        });
    }

    /**
     * Constructs and returns a {@link Neighborhood} record for a given group.
     */
    public void getGroupNeighborhood (final int groupId, ResultListener<Neighborhood> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Neighborhood>(listener) {
            public Neighborhood invokePersistResult() throws PersistenceException {
                Neighborhood hood = new Neighborhood();
                // first load the center group data
                hood.group = makeNeighborGroup(_groupRepo.loadGroup(groupId));
                hood.group.members = _groupRepo.countMembers(groupId);

                // we have no other groups
                hood.neighborGroups = new NeighborGroup[0];

                // but we're including all the group's members, so load'em
                Collection<GroupMembershipRecord> gmRecs = _groupRepo.getMembers(groupId);
                int[] memberIds = new int[gmRecs.size()];
                int ii = 0;
                for (GroupMembershipRecord gmRec : gmRecs) {
                    memberIds[ii ++] = gmRec.memberId;
                }

                List<NeighborMember> members = new ArrayList<NeighborMember>();
                for (NeighborFriendRecord fRec : _memberRepo.getNeighborhoodMembers(memberIds)) {
                    members.add(makeNeighborMember(fRec));
                }
                hood.neighborMembers = members.toArray(new NeighborMember[0]);
                return hood;
            }
            
            // after we finish, have main thread go through
            public void handleSuccess () {
                // set online status and figure out populations
                finalizeNeighborhood(_result);
                _listener.requestCompleted(_result);
            }
        });
    }

    
    // from interface MemberProvider
    public void alterFriend (ClientObject caller, int friendId, boolean add,
                             final InvocationService.InvocationListener lner)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);
        ResultListener<Void> rl = new ResultListener<Void>() {
            public void requestCompleted (Void result) {
                // that's cool
            }
            public void requestFailed (Exception cause) {
                lner.requestFailed(cause.getMessage());
            }
        };
        alterFriend(user, user.getMemberId(), friendId, add, rl);
    }

    // from interface MemberProvider
    public void getHomeId (ClientObject caller, byte ownerType, int ownerId,
                          final InvocationService.ResultListener listener)
        throws InvocationException
    {
        ResultListener<Integer> rl = new ResultListener<Integer>() {
            public void requestCompleted (Integer result) {
                listener.requestProcessed(result);
            }
            public void requestFailed (Exception cause) {
                listener.requestFailed(cause.getMessage());
            }
        };
        getHomeId(ownerType, ownerId, rl);
    }

    // from interface MemberProvider
    public void setAvatar (
        ClientObject caller, int avatarItemId, final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        if (avatarItemId == 0) {
            // a request to return to the default avatar
            finishSetAvatar(user, null, listener);
            return;
        }

        // otherwise, make sure it exists and we own it
        MsoyServer.itemMan.getItem(
            new ItemIdent(Item.AVATAR, avatarItemId), new ResultListener<Item>() {
            public void requestCompleted (Item item) {
                Avatar avatar = (Avatar) item;
                // ensure that they own it!
                if (user.getMemberId() != avatar.ownerId) {
                    requestFailed(new Exception("An avatar that the user " +
                        "does not own was specified!"));
                } else {
                    finishSetAvatar(user, avatar, listener);
                }
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Unable to retrieve user's avatar.", cause);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // from interface MemberProvider
    public void setDisplayName (ClientObject caller, final String name,
                                final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        // TODO: verify entered string

        MsoyServer.invoker.postUnit(new RepositoryUnit("setDisplayName") {
            public void invokePersist () throws PersistenceException {
                _memberRepo.configureDisplayName(user.getMemberId(), name);
            }
            public void handleSuccess () {
                user.setMemberName(new MemberName(name, user.getMemberId()));
                updateOccupantInfo(user);
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to set display name [user=" + user.which() +
                            ", name='" + name + "', error=" + pe + "].");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // from interface MemberProvider
    public void purchaseRoom (ClientObject caller, final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);
        final int memberId = user.getMemberId();
        final String roomName = user.memberName + "'s new room";

        // TODO: charge some flow

        MsoyServer.invoker.postUnit(new RepositoryUnit("purchaseRoom") {
            public void invokePersist () throws PersistenceException {
                _newRoomId = MsoyServer.sceneRepo.createBlankRoom(MsoySceneModel.OWNER_TYPE_MEMBER,
                    memberId, roomName);
            }
            public void handleSuccess () {
                user.addToOwnedScenes(new SceneBookmarkEntry(_newRoomId, roomName, 0));
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to create a new room [user=" + user.which() +
                            ", error=" + pe + "].");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
            protected int _newRoomId;
        });
    }
    
    /**
     * Grant a member some flow, categorized and optionally metatagged with an action
     * type and a detail String. The member's {@link MemberRecord} is updated, as is the
     * {@link DailyFlowGrantedRecord}. Finally, a line is written to the flow grant log.
     */
    public void grantFlow (final MemberName member, final int amount,
                           final UserAction grantAction, final String details)
    {
        // if the member is logged on, make sure we can update their flow
        final MemberObject mObj = MsoyServer.lookupMember(member);
        MsoyServer.invoker.postUnit(new RepositoryUnit("grantFlow") {
            public void invokePersist () throws PersistenceException {
                _memberRepo.getFlowRepository().grantFlow(member.getMemberId(), amount);
                if (mObj != null) {
                    _flow = _memberRepo.loadMember(member.getMemberId()).flow;
                }
            }
            public void handleSuccess () {
                flowLog.info(System.currentTimeMillis() + " " + member.getMemberId() + " G " +
                             amount + " " + (grantAction != null ? grantAction : "null") +
                             (details != null ? " " + details : ""));
                // TODO: distributed considerations
                if (mObj != null) {
                    mObj.setFlow(_flow);
                }
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to grant flow [member=" + member + ", grantAction=" +
                            grantAction + ", amount=" + amount + ", details=" + details + "]");
            }
            public int _flow;
        });
    }

    /**
     * Debit a member some flow, categorized and optionally metatagged with an action
     * type and a detail String. The member's {@link MemberRecord} is updated, as is the
     * {@link DailyFlowSpentRecord}. Finally, a line is written to the flow grant log.
     */
    public void spendFlow (final MemberName member, final int amount,
                           final UserAction spendAction, final String details)
    {
        // if the member is logged on, make sure we can update their flow
        final MemberObject mObj = MsoyServer.lookupMember(member);
        MsoyServer.invoker.postUnit(new RepositoryUnit("spendFlow") {
            public void invokePersist () throws PersistenceException {
                _memberRepo.getFlowRepository().spendFlow(member.getMemberId(), amount);
                if (mObj != null) {
                    _flow = _memberRepo.loadMember(member.getMemberId()).flow;
                }
            }
            public void handleSuccess () {
                flowLog.info(System.currentTimeMillis() + " " + member.getMemberId() + " S " +
                             amount + " " + (spendAction != null ? spendAction : "null") +
                             (details != null ? " " + details : ""));
                // if the member is logged on, update their object
                // TODO: distributed considerations
                if (mObj != null) {
                    mObj.setFlow(_flow);
                }
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to spend flow [member=" + member + ", spendAction=" +
                            spendAction + ", amount=" + amount + ", details=" + details + "]");
            }
            public int _flow;
        });
    }

    /**
     * Register and log an action taken by a specific user for humanity assessment
     * and conversion analysis purposes.  
     */
    public void logUserAction (final MemberName member, final UserAction action,
                               final String details)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("takeAction") {
            public void invokePersist () throws PersistenceException {
                _memberRepo.getFlowRepository().logMemberAction(
                    member.getMemberId(), action.getNumber(), details);
            }
            public void handleSuccess () {
                // yay
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to note user action[member=" + member + ", action=" +
                            action + ", details=" + details + "]");
            }
        });
    }
    
    /**
     * Iterates over all the lobbies and the scenes in the world at the moment, find out the n most
     * populated ones and sort all scenes by owner. cache the values.  TODO: this is currently O(N
     * log N) in the number of rooms; if that is unrealistic in the long run, we can easily make it
     * O(N) by just bubbling into the top 20 (whatever) rooms on the fly, as we enumerate the scene
     * managers.
     */
    protected void updatePPCache ()
    {
        if (System.currentTimeMillis() - _popularPlaceStamp <= POPULAR_PLACES_CACHE_LIFE) {
            return;
        }
        _scenesByOwner = new HashMap<IntTuple, PopularPlace>();
        _topPlaces = new LinkedList<PopularPlace>();
        Iterator<?> i = MsoyServer.plreg.enumeratePlaceManagers();
        while (i.hasNext()) {
            PlaceManager plMgr = (PlaceManager) i.next();
            int count = plMgr.getPlaceObject().occupantInfo.size();
            if (plMgr instanceof RoomManager) {
                MsoyScene scene = (MsoyScene) ((RoomManager) plMgr).getScene();
                MsoySceneModel model = (MsoySceneModel) scene.getSceneModel();
                IntTuple owner = new IntTuple(model.ownerType, model.ownerId);
                PopularScenePlace place = (PopularScenePlace) _scenesByOwner.get(owner);
                if (place == null) {
                    if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                        place = new PopularGroupPlace();
                        ((PopularGroupPlace) place).groupId = model.ownerId;
                    } else if (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                        place = new PopularMemberPlace();
                        ((PopularMemberPlace) place).memberId = model.ownerId;
                    } else {
                        throw new IllegalArgumentException(
                            "unknown owner type: " + model.ownerType);
                    }
                    // make sure we immediately acquire a sceneId
                    place.scenePop = -1;
                    // and update the main data structures
                    _scenesByOwner.put(owner, place);
                    _topPlaces.add(place);
                }
                if (count > place.scenePop) {
                    place.name = scene.getName();
                    place.sceneId = scene.getId();
                    place.scenePop = count;
                }
                place.population += count;
            } else if (plMgr instanceof LobbyManager) {
                PopularGamePlace place = new PopularGamePlace();
                place.name = ((LobbyConfig) plMgr.getConfig()).game.name;
                place.gameId = ((LobbyManager) plMgr).getGameId();
                place.population = count;
                _topPlaces.add(place);
            }
        }
        Collections.sort(_topPlaces, new Comparator<PopularPlace>() {
            public int compare (PopularPlace o1, PopularPlace o2) {
                return o1.population < o2.population ? -1 : o1.population == o2.population ? 0 : 1;
            }
        });
        _popularPlaceStamp = System.currentTimeMillis();
    }

    /**
     * Generic alterFriend() functionality for the two public methods above. Please note that user
     * can be null here (i.e. offline).
     */
    protected void alterFriend (final MemberObject user, final int userId, final int friendId,
                                final boolean add, ResultListener<Void> lner)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>("alterFriend", lner) {
            public Void invokePersistResult () throws PersistenceException {
                if (add) {
                    _entry = _memberRepo.inviteOrApproveFriend(userId, friendId);
                    if (user != null) {
                        _userName = user.memberName;
                    } else {
                        _userName = _memberRepo.loadMember(userId).getName();
                    }
                } else {
                    _memberRepo.removeFriends(userId, friendId);
                }
                return null;
            }

            public void handleSuccess () {
                FriendEntry oldEntry = user != null ? user.friends.get(friendId) : null;
                MemberName friendName = (oldEntry != null) ?
                    oldEntry.name : (_entry != null ? _entry.name : null);
                MemberObject friendObj = (friendName != null) ?
                    MsoyServer.lookupMember(friendName) : null;

                // update ourselves and the friend
                if (!add || _entry == null) {
                    // remove the friend
                    if (oldEntry != null) {
                        if (user != null) {
                            user.removeFromFriends(friendId);
                        }
                        if (friendObj != null) {
                            friendObj.removeFromFriends(userId);
                        }
                    }

                } else {
                    // add or update the friend/status
                    _entry.online = (friendObj != null);
                    byte oppStatus = getOppositeFriendStatus(_entry.status);
                    if (oldEntry == null) {
                        if (user != null) {
                            user.addToFriends(_entry);
                        }
                        if (friendObj != null) {
                            FriendEntry opp = new FriendEntry(_userName, user != null, oppStatus);
                            friendObj.addToFriends(opp);
                        }

                    } else {
                        if (user != null) {
                            user.updateFriends(_entry);
                        }
                        if (friendObj != null) {
                            FriendEntry opp = friendObj.friends.get(userId);
                            opp.status = oppStatus;
                            friendObj.updateFriends(opp);
                        }
                    }
                }
                _listener.requestCompleted(null);
            }

            protected FriendEntry _entry;
            protected MemberName _userName;
        });
    }

    /**
     * Convenience method to ensure that the specified caller is not a guest.
     */
    protected void ensureNotGuest (MemberObject caller)
        throws InvocationException
    {
        if (caller.isGuest()) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
    }

    /**
     * Return the status of a friendship as viewed from the other side.
     */
    protected byte getOppositeFriendStatus (byte status)
    {
        switch (status) {
        case FriendEntry.PENDING_MY_APPROVAL:
            return FriendEntry.PENDING_THEIR_APPROVAL;

        case FriendEntry.PENDING_THEIR_APPROVAL:
            return FriendEntry.PENDING_MY_APPROVAL;

        default:
            return FriendEntry.FRIEND;
        }
    }

    /**
     * Finish configuring the user's avatar.
     *
     * @param avatar may be null to revert to the default member avatar.
     */
    protected void finishSetAvatar (
        final MemberObject user, final Avatar avatar,
        final InvocationService.InvocationListener listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("setAvatarPt2") {
            public void invokePersist () throws PersistenceException {
                _memberRepo.configureAvatarId(user.getMemberId(),
                    (avatar == null) ? 0 : avatar.itemId);
            }

            public void handleSuccess () {
                MsoyServer.itemMan.updateItemUsage(
                    user.getMemberId(), user.avatar, avatar, new ResultListener.NOOP<Object>() {
                    public void requestFailed (Exception cause) {
                        log.warning("Unable to update usage from an avatar change.");
                    }
                });
                user.setAvatar(avatar);
                updateOccupantInfo(user);
            }

            public void handleFailure (Exception pe) {
                log.warning("Unable to set avatar [user=" + user.which() +
                            ", avatar='" + avatar + "', " + "error=" + pe + "].");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });

    }

    // Figure out the population of the various rooms associated with a neighborhood;
    // a group's and a member's home scenes. This must be called on the dobj thread.
    protected void finalizeNeighborhood (Neighborhood hood)
    {
        updatePPCache();
        if (hood.member != null) {
            finalizeEntity(hood.member);
        }
        if (hood.group != null) {
            finalizeEntity(hood.group);
        }
        for (NeighborGroup group : hood.neighborGroups) {
            finalizeEntity(group);
        }
        for (NeighborMember friend : hood.neighborMembers) {
            finalizeEntity(friend);
        }
    }

    // set the population of a neighbour group
    protected void finalizeEntity (NeighborGroup group)
    {
        PopularPlace place = getPopularPlace(MsoySceneModel.OWNER_TYPE_GROUP, group.group.groupId);
        group.population = place != null ? place.population : 0;
    }

    // set the population of a neighbour friend and figure out if it's online
    protected void finalizeEntity (NeighborMember friend)
    {
        int memberId = friend.member.getMemberId();
        PopularPlace place = getPopularPlace(MsoySceneModel.OWNER_TYPE_MEMBER, memberId);
        friend.population = place != null ? place.population : 0;
        friend.isOnline = MsoyServer.lookupMember(memberId) != null;
    }

    protected NeighborGroup makeNeighborGroup (GroupRecord gRec) throws PersistenceException
    {
        NeighborGroup nGroup = new NeighborGroup();
        nGroup.group = new GroupName(gRec.name, gRec.groupId);
        nGroup.homeSceneId = gRec.homeSceneId;
        if (gRec.logoMediaHash != null) { 
            nGroup.logo = new MediaDesc(gRec.logoMediaHash, gRec.logoMimeType);
        }
        nGroup.members = _groupRepo.countMembers(gRec.groupId);
        return nGroup;
    }

    // convert a {@link NeighborFriendRecord} to a {@link NeighborMember}.
    protected NeighborMember makeNeighborMember (NeighborFriendRecord fRec)
    {
        NeighborMember nFriend = new NeighborMember();
        nFriend.member = new MemberName(fRec.name, fRec.memberId);
        nFriend.created = new Date(fRec.created.getTime());
        nFriend.flow = fRec.flow;
        nFriend.homeSceneId = fRec.homeSceneId;
        nFriend.lastSession = fRec.lastSession;
        nFriend.sessionMinutes = fRec.sessionMinutes;
        nFriend.sessions = fRec.sessions;
        return nFriend;
    }

    // convert a {@link MemberRecord} to a {@link NeighborMember}.
    protected NeighborMember makeNeighborMember (MemberRecord mRec)
    {
        NeighborMember nFriend = new NeighborMember();
        nFriend.member = new MemberName(mRec.name, mRec.memberId);
        nFriend.created = new Date(mRec.created.getTime());
        nFriend.flow = mRec.flow;
        nFriend.homeSceneId = mRec.homeSceneId;
        nFriend.lastSession = mRec.lastSession;
        nFriend.sessionMinutes = mRec.sessionMinutes;
        nFriend.sessions = mRec.sessions;
        return nFriend;
    }

    // handcrafted JSON serialization, to minimize the overhead
    protected JSONObject toJSON (Neighborhood hood)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        if (hood.member != null) {
            obj.put("member", toJSON(hood.member));
        }
        if (hood.group != null) {
            obj.put("group", toJSON(hood.group));
        }
        JSONArray jArr = new JSONArray();
        for (NeighborMember friend : hood.neighborMembers) {
            jArr.put(toJSON(friend));
        }
        obj.put("friends", jArr);
        jArr = new JSONArray();
        for (NeighborGroup group : hood.neighborGroups) {
            jArr.put(toJSON(group));
        }
        obj.put("groups", jArr);
        return obj;
    }

    protected JSONObject toJSON (NeighborMember member)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put("name", member.member.toString());
        obj.put("id", member.member.getMemberId());
        obj.put("isOnline", member.isOnline);
        obj.put("pop", member.population);
        obj.put("created", member.created.getTime());
        obj.put("sNum", member.sessions);
        obj.put("sMin", member.sessionMinutes);
        obj.put("lastSess", member.lastSession.getTime());
        return obj;
    }

    protected JSONObject toJSON (NeighborGroup group)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put("name", group.group.groupName);
        obj.put("id", group.group.groupId);
        obj.put("members", group.members);
        obj.put("pop", group.population);
        if (group.logo != null) {
            obj.put("logo", group.logo.toString());
        }
        return obj;
    }

    /** A mapping of ownerType/ownerId tuples to sets of scene ID's. Cached. */
    protected Map<IntTuple, PopularPlace> _scenesByOwner;
    /** A list of every place (lobby or scene) in the world, sorted by population. */
    protected List<PopularPlace> _topPlaces;
    /** The time when the cached values were last calculated. */
    protected long _popularPlaceStamp;

    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;
    
    /** Provides access to persistent group data. */
    protected GroupRepository _groupRepo;
}
