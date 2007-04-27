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

import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.server.GameManager;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.whirled.server.SceneManager;

import com.threerings.msoy.data.PopularPlace;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.SceneBookmarkEntry;
import com.threerings.msoy.data.Neighborhood.NeighborEntity;
import com.threerings.msoy.data.Neighborhood.NeighborGroup;
import com.threerings.msoy.data.Neighborhood.NeighborMember;
import com.threerings.msoy.data.PopularPlace.*;
import com.threerings.msoy.data.PopularPlace.PopularPlaceOwner.OwnerType;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.server.LobbyManager;
import com.threerings.msoy.game.server.MsoyGameManager;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.web.data.FriendInviteObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.WorldMemberInfo;

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
    public static void queueFlowUpdated (final int memberId, final int newFlow)
    {
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.flowUpdated(memberId, newFlow);
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
    public void flowUpdated (int memberId, int newFlow)
    {
        MemberObject user = MsoyServer.lookupMember(memberId);
        if (user != null) {
            user.setFlow(newFlow);
        }
    }

    /**
     * Export alterFriend() functionality according to the web servlet way of doing things. 
     */
    public void alterFriend (int userId, int friendId, boolean add,
                             ResultListener<Void> listener)
    {
        alterFriend(MsoyServer.lookupMember(userId), userId, friendId, add, listener);
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
        ClientObject caller, int avatarItemId, final float newScale,
        final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        if (avatarItemId == 0) {
            // a request to return to the default avatar
            finishSetAvatar(user, null, newScale, listener);
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
                    finishSetAvatar(user, avatar, newScale, listener);
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

        // figure out if they want a group or a personal room
        SceneManager sceneMan = MsoyServer.screg.getSceneManager(user.sceneId);
        MsoyScene scene = (MsoyScene) sceneMan.getScene();
        if (!scene.canEdit(user)) {
            throw new InvocationException(InvocationCodes.E_ACCESS_DENIED);
        }
        MsoySceneModel model = (MsoySceneModel) scene.getSceneModel();
        boolean isGroup = (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP);

        final byte ownerType = isGroup ? MsoySceneModel.OWNER_TYPE_GROUP
                                       : MsoySceneModel.OWNER_TYPE_MEMBER;
        final int ownerId = isGroup ? model.ownerId : user.getMemberId();
        final String roomName = isGroup ? "New 'somegroup' room"
                                        : (user.memberName + "'s new room");

        // TODO: charge some flow

        MsoyServer.invoker.postUnit(new RepositoryUnit("purchaseRoom") {
            public void invokePersist () throws PersistenceException {
                _newRoomId = MsoyServer.sceneRepo.createBlankRoom(ownerType, ownerId, roomName);
            }
            public void handleSuccess () {
                user.addToOwnedScenes(new SceneBookmarkEntry(_newRoomId, roomName, 0));
                listener.requestProcessed();
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to create a new room [user=" + user.which() +
                            ", error=" + pe + ", cause=" + pe.getCause() + "].");
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
        fillIn(group, (PopularScenePlace) _placesByOwner.get(
            new PopularPlaceOwner(OwnerType.GROUP, group.group.getGroupId())));
    }

    /**
     * Fill in the popSet and popCount members of a {@link NeighborMember} instance.
     * 
     * This must be called on the dobj thread.
     */
    public void fillIn (NeighborMember member)
    {
        updatePPCache();
        fillIn(member, (PopularScenePlace) _placesByOwner.get(
            new PopularPlaceOwner(OwnerType.MEMBER, member.member.getMemberId())));
    }

    /**
     * Fill in the popSet and popCount members of a {@link NeighborEntity} instance
     * given the supplied, associated {@link PopularPlace}.
     * 
     * This must be called on the dobj thread.
     * 
     * TODO: If we're going to bother showing the names of people present, we have to
     * do it properly: this only looks at a single Place, where the population count is
     * taken from every Place with a given owner (person, group or game); thus we could
     * have a plaque showing population 20 but only list three names. That's silly.
     */
    protected void fillIn (NeighborEntity entity, PopularScenePlace place)
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
        _placesByOwner = new HashMap<PopularPlaceOwner, PopularPlace>();
        _topPlaces = new LinkedList<PopularPlace>();
        _totalPopulation = 0;
        Iterator<?> i = MsoyServer.plreg.enumeratePlaceManagers();
        while (i.hasNext()) {
            PlaceManager plMgr = (PlaceManager) i.next();
            PlaceObject plObj = plMgr.getPlaceObject();

            int count = 0;
            for (OccupantInfo info : plObj.occupantInfo) {
                if (info instanceof WorldMemberInfo) {
                    count ++;
                }
            }
            // don't track places without members in them at all
            if (count == 0) {
                continue;
            }

            if (plMgr instanceof SceneManager) {
                SceneManager rMgr = ((SceneManager) plMgr);
                MsoyScene scene = (MsoyScene) rMgr.getScene();
                MsoySceneModel model = (MsoySceneModel) scene.getSceneModel();

                boolean isGroup;
                if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                    isGroup = true;
                } else if (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                    isGroup = false;
                } else {
                    throw new IllegalArgumentException("unknown owner type: " + model.ownerType);
                }

                PopularPlaceOwner owner = new PopularPlaceOwner(
                    isGroup ? OwnerType.GROUP : OwnerType.MEMBER, model.ownerId);
                PopularScenePlace place = (PopularScenePlace) _placesByOwner.get(owner);
                if (place == null) {
                    place = isGroup ? new PopularGroupPlace(rMgr) : new PopularMemberPlace(rMgr);
                    // update the main data structures
                    _placesByOwner.put(owner, place);
                    _topPlaces.add(place);
                }

                if (place.plMgr == null ||
                    count > place.plMgr.getPlaceObject().occupantInfo.size()) {
                    place.plMgr = rMgr;
                }
                place.population += count;
                _totalPopulation += count;

            } else if (plMgr instanceof MsoyGameManager) {
                MsoyGameConfig config = ((MsoyGameConfig) ((GameManager) plMgr).getGameConfig());
                PopularPlaceOwner owner =
                    new PopularPlaceOwner(OwnerType.GAME, config.persistentGameId);
                PopularGamePlace place = (PopularGamePlace) _placesByOwner.get(owner);
                if (place == null) {
                    place = new PopularGamePlace(config.name, config.persistentGameId);
                    _placesByOwner.put(owner, place);
                    _topPlaces.add(place);
                }
                
                place.population += count;
                _totalPopulation += count;
            }
        }

        // let's iterate over the lobbies too, which are no longer places
        i = MsoyServer.lobbyReg.enumerateLobbyManagers();
        while (i.hasNext()) {
            LobbyObject lObj = ((LobbyManager) i.next()).getLobbyObject();
            
            // then add up the population count for each table being formed in this lobby
            int count = 0;
            for (Table table : lObj.tables) {
                count += table.getOccupiedCount();
            }
            
            // we skip empty lobbies, obviously
            if (count == 0) {
                continue;
            }

            // otherwise we're most likely dealing with a game that's already been registered
            Game game = lObj.game;
            PopularPlaceOwner owner = new PopularPlaceOwner(OwnerType.GAME, game.getPrototypeId());
            PopularGamePlace place = (PopularGamePlace) _placesByOwner.get(owner);

            if (place == null) {
                // or sometimes, there's somebody in the lobby of a game that nobody is playing
                place = new PopularGamePlace(game.name, game.getPrototypeId());
                _placesByOwner.put(owner, place);
                _topPlaces.add(place);
            }
            
            place.population += count;
            // we don't increase total population: these are people we've already counted once
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
        final MemberObject user, final Avatar avatar, final float newScale,
        final InvocationService.InvocationListener listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("setAvatarPt2") {
            public void invokePersist () throws PersistenceException {
                _memberRepo.configureAvatarId(user.getMemberId(),
                    (avatar == null) ? 0 : avatar.itemId);
                if (newScale != 0 && avatar != null && avatar.scale != newScale) {
                    MsoyServer.itemMan.getAvatarRepository().updateScale(avatar.itemId, newScale);
                }
            }

            public void handleSuccess () {
                if (newScale != 0 && avatar != null) {
                    avatar.scale = newScale;
                    MsoyServer.itemMan.updateUserCache(avatar);
                }
                MsoyServer.itemMan.updateItemUsage(
                    user.getMemberId(), user.avatar, avatar, new ResultListener.NOOP<Object>() {
                    public void requestFailed (Exception cause) {
                        log.warning("Unable to update usage from an avatar change.");
                    }
                });
                user.setAvatar(avatar);
                user.avatarState = null; // clear out the state
                updateOccupantInfo(user);
            }

            public void handleFailure (Exception pe) {
                log.warning("Unable to set avatar [user=" + user.which() +
                            ", avatar='" + avatar + "', " + "error=" + pe + "].");
                log.log(Level.WARNING, "", pe);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });

    }

    /** A mapping of ownerType/ownerId tuples to sets of scene ID's. Cached. */
    protected Map<PopularPlaceOwner, PopularPlace> _placesByOwner;
    
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
