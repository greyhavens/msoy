//
// $Id$

package com.threerings.msoy.server;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;

import org.json.JSONArray;
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

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.msoy.data.PopularPlace;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.SceneBookmarkEntry;
import com.threerings.msoy.data.Neighborhood.NeighborEntity;
import com.threerings.msoy.data.Neighborhood.NeighborGroup;
import com.threerings.msoy.data.Neighborhood.NeighborMember;
import com.threerings.msoy.data.PopularPlace.*;
import com.threerings.msoy.game.server.LobbyManager;
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.web.data.FriendEntry;
import com.threerings.msoy.web.data.FriendInviteObject;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.WorldMemberInfo;
import com.threerings.msoy.world.server.RoomManager;

import com.threerings.msoy.person.server.persist.MailMessageRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.GroupRepository;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * Manage msoy members.
 */
public class MemberManager
    implements MemberProvider
{
    /** Cache popular place computations for five seconds while we're debugging. */
    public static final long POPULAR_PLACES_CACHE_LIFE = 5*1000;

    /** The maximum number of named members to list for a place. */
    public static final int NAMED_MEMBERS_IN_POPULAR_PLACE = 8;
    
    /**
     * This can be called from any thread to queue an update of the member's current flow if they
     * are online.
     */
    public static void queueFlowUpdated (final int memberId, final int flow)
    {
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.flowUpdated(memberId, flow);
            }
        });
    }

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
     * Called when a member's flow is updated. If they are online we update {@link
     * MemberObject#flow}.
     */
    public void flowUpdated (int memberId, int flow)
    {
        MemberObject user = MsoyServer.lookupMember(memberId);
        if (user != null) {
            user.setFlow(flow);
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
     * Return a JSON-serialized version of the Popular Places data structure.
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
                obj.put("name", place.getName());
                obj.put("pop", place.population);
                obj.put("id", place.getId());
                if (place instanceof PopularGamePlace) {
                    games.put(obj);
                } else {
                    obj.put("sceneId", ((PopularScenePlace) place).getSceneId());
                    if (place instanceof PopularMemberPlace) {
                        friends.put(obj);
                    } else {
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
            result.put("games", games);
            result.put("totpop", _totalPopulation);
            listener.requestCompleted(URLEncoder.encode(result.toString(), "UTF-8"));
        } catch (Exception e) {
            listener.requestFailed(e);
        }
    }

    // from interface MemberProvider
    public void alterFriend (ClientObject caller, int friendId, boolean add,
                             final InvocationService.ConfirmListener lner)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);
        ResultListener<Void> rl = new ResultListener<Void>() {
            public void requestCompleted (Void result) {
                lner.requestProcessed();
            }
            public void requestFailed (Exception cause) {
                lner.requestFailed(cause.getMessage());
            }
        };
        if (add) {
            // an 'add friend' request turns into an invitational mail message
            MailMessageRecord mailRecord = new MailMessageRecord();
            mailRecord.senderId = user.memberName.getMemberId();
            mailRecord.recipientId = friendId;
            mailRecord.subject = "Be my Friend";
            mailRecord.payloadType = MailPayload.TYPE_FRIEND_INVITE;

            MsoyServer.mailMan.deliverMessage(
                user.memberName.getMemberId(), friendId, "Be My Friend",
                null, new FriendInviteObject(), rl);

        } else {
            alterFriend(user, user.getMemberId(), friendId, add, rl);
        }
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
    public void grantFlow (final int memberId, final int amount,
                           final UserAction grantAction, final String details)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("grantFlow") {
            public void invokePersist () throws PersistenceException {
                _flow = _memberRepo.getFlowRepository().updateFlow(
                    memberId, amount, grantAction.toString() + " " + details, true);
            }
            public void handleSuccess () {
                flowUpdated(memberId, _flow);
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to grant flow [memberId=" + memberId +
                        ", grantAction=" + grantAction + ", amount=" + amount + ", details=" +
                        details + "]", pe);
            }
            protected int _flow;
        });
    }

    /**
     * Debit a member some flow, categorized and optionally metatagged with an action
     * type and a detail String. The member's {@link MemberRecord} is updated, as is the
     * {@link DailyFlowSpentRecord}. Finally, a line is written to the flow grant log.
     */
    public void spendFlow (final int memberId, final int amount,
                           final UserAction spendAction, final String details)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("spendFlow") {
            public void invokePersist () throws PersistenceException {
                _flow = _memberRepo.getFlowRepository().updateFlow(
                    memberId, amount, spendAction.toString() + " " + details, false);
            }
            public void handleSuccess () {
                flowUpdated(memberId, _flow);
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to spend flow [memberId=" + memberId +
                        ", grantAction=" + spendAction + ", amount=" + amount +
                        ", details=" + details + "]", pe);
            }
            protected int _flow;
        });
    }

    /**
     * Register and log an action taken by a specific user for humanity assessment and conversion
     * analysis purposes. Some actions grant flow as a result of being taken, this method handles
     * that granting and updating the member's flow if they are online.
     */
    public void logUserAction (MemberName member, final UserAction action, final String details)
    {
        final int memberId = member.getMemberId();
        MsoyServer.invoker.postUnit(new RepositoryUnit("takeAction") {
            public void invokePersist () throws PersistenceException {
                // record that that took the action
                _flow = _memberRepo.getFlowRepository().logUserAction(memberId, action, details);
            }
            public void handleSuccess () {
                if (_flow > 0) {
                    flowUpdated(memberId, _flow);
                }
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to note user action [memberId=" + memberId +
                            ", action=" + action + ", details=" + details + "]");
            }
            protected int _flow;
        });
    }
    
    /**
     * Fill in the popSet and popCount members of a {@link NeighborGroup} instance.
     * 
     * This must be called on the dobj thread.
     */
    public void fillIn (NeighborGroup group)
    {
        updatePPCache();
        fillIn(group, _scenesByOwner.get(
            new IntTuple(MsoySceneModel.OWNER_TYPE_GROUP, group.group.groupId)));
    }

    /**
     * Fill in the popSet and popCount members of a {@link NeighborMember} instance.
     * 
     * This must be called on the dobj thread.
     */
    public void fillIn (NeighborMember member)
    {
        updatePPCache();
        fillIn(member, _scenesByOwner.get(
            new IntTuple(MsoySceneModel.OWNER_TYPE_MEMBER, member.member.getMemberId())));
    }

    /**
     * Fill in the popSet and popCount members of a {@link NeighborEntity} instance
     * given the supplied, associated {@link PopularPlace}.
     * 
     * This must be called on the dobj thread.
     */
    protected void fillIn (NeighborEntity entity, PopularPlace place)
    {
        if (place == null) {
            entity.popSet = null;
            entity.popCount = 0;
            return;
        }

        entity.popSet = new HashSet<MemberName>();
        int cnt = NAMED_MEMBERS_IN_POPULAR_PLACE;
        for (OccupantInfo info : place.plMgr.getPlaceObject().occupantInfo) {
            // only count members
            if (info instanceof WorldMemberInfo) {
                entity.popSet.add((MemberName) info.username);
                if (--cnt == 0) {
                    break;
                }
            }
        }
        entity.popCount = place.plMgr.getPlaceObject().occupantInfo.size();
    }
    
    /**
     * Iterates over all the lobbies and the scenes in the world at the moment, find out the n most
     * populated ones and sort all scenes by owner. cache the values.
     * 
     * This must be called on the dobj thread.
     */
    protected void updatePPCache ()
    {
        if (System.currentTimeMillis() - _popularPlaceStamp <= POPULAR_PLACES_CACHE_LIFE) {
            return;
        }
        _scenesByOwner = new HashMap<IntTuple, PopularPlace>();
        _topPlaces = new LinkedList<PopularPlace>();
        _totalPopulation = 0;
        Iterator<?> i = MsoyServer.plreg.enumeratePlaceManagers();
        while (i.hasNext()) {
            PlaceManager plMgr = (PlaceManager) i.next();
            int count = 0;
            for (OccupantInfo info : plMgr.getPlaceObject().occupantInfo) {
                if (info instanceof WorldMemberInfo) {
                    count ++;
                }
            }
            // don't track places without members in them at all
            if (count == 0) {
                continue;
            }
            if (plMgr instanceof RoomManager) {
                MsoyScene scene = (MsoyScene) ((RoomManager) plMgr).getScene();
                MsoySceneModel model = (MsoySceneModel) scene.getSceneModel();
                IntTuple owner = new IntTuple(model.ownerType, model.ownerId);
                PopularScenePlace place = (PopularScenePlace) _scenesByOwner.get(owner);
                if (place == null) {
                    if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                        place = new PopularGroupPlace((RoomManager) plMgr);
                    } else if (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                        place = new PopularMemberPlace((RoomManager) plMgr);
                    } else {
                        throw new IllegalArgumentException(
                            "unknown owner type: " + model.ownerType);
                    }
                    // update the main data structures
                    _scenesByOwner.put(owner, place);
                    _topPlaces.add(place);
                }
                if (place.plMgr == null ||
                    count > place.plMgr.getPlaceObject().occupantInfo.size()) {
                    place.plMgr = plMgr;
                }
                place.population += count;
                _totalPopulation += count;
            } else if (plMgr instanceof LobbyManager) {
                PopularGamePlace place = new PopularGamePlace((LobbyManager) plMgr);
                place.population = count;
                _topPlaces.add(place);
                _totalPopulation += count;
            }
        }
        /*
         * TODO: this is currently O(N log N) in the number of rooms; if that is unrealistic in
         * the long run, we can easily make it O(N) by just bubbling into the top 20 (whatever)
         * rooms on the fly, as we enumerate the scene managers.
         * */
        Collections.sort(_topPlaces, new Comparator<PopularPlace>() {
            public int compare (PopularPlace o1, PopularPlace o2) {
                return o1.population > o2.population ? -1 : o1.population == o2.population ? 0 : 1;
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
                    _entry = _memberRepo.inviteFriend(userId, friendId);
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
                    if (oldEntry == null) {
                        if (user != null) {
                            user.addToFriends(_entry);
                        }
                        if (friendObj != null) {
                            FriendEntry opp = new FriendEntry(_userName, user != null);
                            friendObj.addToFriends(opp);
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

    /** A mapping of ownerType/ownerId tuples to sets of scene ID's. Cached. */
    protected Map<IntTuple, PopularPlace> _scenesByOwner;
    
    /** A list of every place (lobby or scene) in the world, sorted by population. */
    protected List<PopularPlace> _topPlaces;

    /** The total number of people in the whirled. */
    protected int _totalPopulation;

    /** The time when the cached values were last calculated. */
    protected long _popularPlaceStamp;
    
    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;
    
    /** Provides access to persistent group data. */
    protected GroupRepository _groupRepo;
}
