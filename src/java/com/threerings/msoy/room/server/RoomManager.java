//
// $Id$

package com.threerings.msoy.room.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.text.MessageUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Comparators;
import com.samskivert.util.ComplainingListener;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.ProxySubscriber;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.server.CrowdObjectAccess;
import com.threerings.crowd.server.LocationManager;

import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.server.SpotSceneManager;

import com.whirled.bureau.data.BureauTypes;

import com.whirled.game.data.PropertySetEvent;
import com.whirled.game.data.PropertySpaceObject.PropertySetException;
import com.whirled.game.server.PropertySpaceDispatcher;
import com.whirled.game.server.PropertySpaceHandler;
import com.whirled.game.server.PropertySpaceHelper;
import com.whirled.game.server.WhirledGameMessageDispatcher;
import com.whirled.game.server.WhirledGameMessageHandler;

import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.BootablePlaceManager;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.bureau.data.WindowClientObject;
import com.threerings.msoy.party.data.PartySummary;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemManager;

import com.threerings.msoy.room.client.RoomService;
import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.AudioData;
import com.threerings.msoy.room.data.Controllable;
import com.threerings.msoy.room.data.ControllableEntity;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.EntityMemories;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.FurniUpdate;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MobObject;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyPortal;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.ObserverInfo;
import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.data.RoomLocal;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesEntry;
import com.threerings.msoy.room.data.RoomPropertiesObject;
import com.threerings.msoy.room.data.SceneAttrsUpdate;
import com.threerings.msoy.room.server.RoomExtras;
import com.threerings.msoy.room.server.persist.MemoriesRecord;
import com.threerings.msoy.room.server.persist.MemoryRepository;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.RoomPropertyRecord;
import com.threerings.msoy.room.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages a "Room".
 */
@EventThread
public class RoomManager extends SpotSceneManager
    implements RoomProvider, BootablePlaceManager
{
    /** Time a room is idle before being unloaded. This is more aggressive than the default. */
    public static long ROOM_IDLE_UNLOAD_PERIOD = 30 * 1000L;

    /**
     * Flush any modified memories contained within the specified Iterable.
     */
    public static void flushMemories (Invoker invoker, final MemoryRepository memoryRepo,
                                      Iterable<EntityMemories> entries)
    {
        if (entries == null) {
            return;
        }
        final List<MemoriesRecord> memrecs = Lists.newArrayList();
        for (EntityMemories entry : entries) {
            if (entry != null && entry.modified) {
                memrecs.add(new MemoriesRecord(entry));
            }
        }
        if (memrecs.size() > 0) {
            invoker.postUnit(new WriteOnlyUnit("storeMemories") {
                public void invokePersist () throws Exception {
                    memoryRepo.storeMemories(memrecs);
                }
            });
        }
    }

    /**
     * Forcibly change the location of the specified body.
     * @return null on success, or an error string.
     */
    public String changeLocation (BodyObject body, MsoyLocation loc)
    {
        try {
            handleChangeLoc(body, loc);
            return null;
        } catch (InvocationException ie) {
            return ie.getMessage(); // return the error string
        }
    }

    /**
     * Forcibly change the state of an actor.
     */
    public void setState (MsoyBodyObject actor, final String state)
    {
        // update the state in their body object
        actor.actorState = state;

        // TODO: consider, instead of updating the whole dang occInfo, dispatching a custom event
        // that will update just the state and serve as the trigger event to usercode...
        updateOccupantInfo(actor.getOid(), new ActorInfo.Updater<ActorInfo>() {
            public boolean update (ActorInfo info) {
                if (ObjectUtil.equals(info.getState(), state)) {
                    return false; // if there was no change, we're done.
                }
                info.setState(state);
                return true;
            }
        });
    }

    /**
     * Called when a member joins a party while they're in our room.
     */
    public void memberJoinedParty (MemberObject memobj, final PartySummary party)
    {
        // add the party summary to the room, if necessary
        if (!_roomObj.parties.containsKey(party.id)) {
            _roomObj.addToParties(party);
        }

        // now we can update their occupant info (it requires that the party summary be set)
        updateOccupantInfo(memobj.getOid(), new MemberInfo.Updater<MemberInfo>() {
            public boolean update (MemberInfo info) {
                return info.updatePartyId(party.id);
            }
        });
    }

    /**
     * Called when a member leaves a party while they're in our room.
     */
    public void memberLeftParty (MemberObject memobj, final int partyId)
    {
        // update their occupant info
        updateOccupantInfo(memobj.getOid(), new MemberInfo.Updater<MemberInfo>() {
            public boolean update (MemberInfo info) {
                return info.updatePartyId(0);
            }
        });

        // remove the party summary if no one remains in this party
        Predicate<OccupantInfo> havePartiers = new Predicate<OccupantInfo>() {
            public boolean apply (OccupantInfo info) {
                return (info instanceof MemberInfo) && ((MemberInfo)info).getPartyId() == partyId;
            }
        };
        if (_roomObj.parties.containsKey(partyId) &&
            !Iterables.any(_occInfo.values(), havePartiers)) {
            _roomObj.removeFromParties(partyId);
        }
    }

    @Override
    public String ratifyBodyEntry (BodyObject body)
    {
        // check to see if the scene permits access
        if (!((MsoyScene) _scene).canEnter((MsoyBodyObject) body)) {
            return InvocationCodes.E_ACCESS_DENIED; // TODO: better? "This room is friend only"
        }

        // if we have a bootlist, check against that
        if (_booted != null && (body instanceof MemberObject)) {
            MemberObject user = (MemberObject) body;
            if (!user.tokens.isSupport() && _booted.contains(user.getMemberId())) {
                return "e.booted";
            }
        }

        return super.ratifyBodyEntry(body);
    }

    // from interface BootablePlaceManager
    public String bootFromPlace (MemberObject user, int booteeId)
    {
        // make sure this user has access to boot
        if (!canManage(user)) {
            return InvocationCodes.E_ACCESS_DENIED;
        }

        // let's look up the user they want to boot
        MemberObject bootee = _locator.lookupMember(booteeId);
        if ((bootee == null) || (bootee.location == null) ||
                (bootee.location.placeOid != _plobj.getOid())) {
            return "e.user_not_present";
        }

        // let's see if the user is another manager
        if (canManage(bootee)) {
            // send a little message to the bootee telling them about the attempt
            SpeakUtil.sendInfo(bootee, MsoyCodes.GENERAL_MSGS,
                MessageUtil.tcompose("m.boot_attempt_mgr", user.getVisibleName()));
            return MessageUtil.tcompose("e.cant_boot_mgr", bootee.getVisibleName());
        }
        // don't let guests get screwed over
        int bootSceneId = bootee.getHomeSceneId();
        if (bootSceneId == _scene.getId()) {
            return InvocationCodes.E_ACCESS_DENIED; // bah, we don't need a better msg
        }

        // success! add them to the boot list
        if (_booted == null) {
            _booted = new ArrayIntSet(1);
        }
        _booted.add(booteeId);

        // and boot them right now.
        SpeakUtil.sendInfo(bootee, MsoyCodes.GENERAL_MSGS,
            MessageUtil.tcompose("m.booted", _scene.getName()));
        _screg.moveBody(bootee, bootSceneId);
        SpeakUtil.sendFeedback(user, MsoyCodes.GENERAL_MSGS, "m.boot_success");

        return null; // indicates success
    }

    /**
     * Can the specified user manage this room.
     */
    public boolean canManage (MemberObject user)
    {
        return ((MsoyScene) _scene).canManage(user);
    }

    /**
     * Checks whether or not the calling user can manage, if so returns the user cast
     * to a MemberObject, throws an {@link InvocationException} if not.
     */
    public MemberObject requireManager (ClientObject caller)
        throws InvocationException
    {
        MemberObject member = (MemberObject) caller;
        if (!canManage(member)) {
            throw new InvocationException(RoomCodes.E_ACCESS_DENIED);
        }
        return member;
    }

    /**
     * Checks whether or not the calling user can bring pets into this room. Returns normally if
     * so, throws an {@link InvocationException} if not.
     */
    public void requireCanAddPet (MemberObject caller)
        throws InvocationException
    {
        if (!canManage(caller)) {
            throw new InvocationException(RoomCodes.E_CANNOT_ADD_PET);
        }
    }

    /**
     * Reclaims an item from this room.
     */
    public void reclaimItem (ItemIdent item, int memberId)
    {
        MsoyScene scene = (MsoyScene)_scene;

        if (item.type == Item.DECOR) {
            // replace the decor with defaults
            SceneAttrsUpdate update = new SceneAttrsUpdate();
            update.init(scene.getId(), scene.getVersion());
            update.name = scene.getName();
            update.decor = MsoySceneModel.defaultMsoySceneModelDecor();
            update.audioData = scene.getAudioData();
            update.entrance = ((MsoySceneModel)scene.getSceneModel()).entrance;
            doRoomUpdate(update, memberId, null);

        } else if (item.type == Item.AUDIO) {
            // clear out the audio
            SceneAttrsUpdate update = new SceneAttrsUpdate();
            update.init(scene.getId(), scene.getVersion());
            update.name = scene.getName();
            update.decor = scene.getDecor();
            update.audioData = null;
            update.entrance = ((MsoySceneModel)scene.getSceneModel()).entrance;
            doRoomUpdate(update, memberId, null);

        } else {
            // find the right furni and pull it out
            for (FurniData furni : scene.getFurni()) {
                if (item.equals(furni.getItemIdent())) {
                    FurniUpdate.Remove update = new FurniUpdate.Remove();
                    update.data = furni;
                    doRoomUpdate(update, memberId, null);
                    break;
                }
            }
        }
    }

    public void occupantLeftAVRGame (MemberObject member)
    {
        // we don't care
    }

    public void occupantEnteredAVRGame (MemberObject member)
    {
        ensureAVRGamePropertySpace(member);
    }

    // documentation inherited from RoomProvider
    public void requestControl (ClientObject caller, ItemIdent item)
    {
        ensureEntityControl(caller, item, "requestControl");
        // TODO: throw invocationexception on failure?
    }

    // documentation inherited from RoomProvider
    public void sendSpriteMessage (ClientObject caller, ItemIdent item, String name, byte[] arg,
                                   boolean isAction)
    {
        // make sure the caller is in the room
        if (caller instanceof MemberObject) {
            MemberObject who = (MemberObject)caller;
            if (!_roomObj.occupants.contains(who.getOid())) {
//                if (!isRecentOccupant(who.getOid())) {
//                    log.warning("Rejecting sprite message request by non-occupant",
//                        "where", where(), "who", who.who(), "left", whenLeft(who.getOid()),
//                        "item", item, "name", name);
//                }
                return;
            }
        }

        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        if (isAction && !ensureEntityControl(caller, item, "triggerAction")) {
            log.info("Dropping sprite message for lack of control", "who", caller.who(),
                "item", item, "name", name);
            return;
        }

        // dispatch this as a simple MessageEvent
        _roomObj.postMessage(RoomCodes.SPRITE_MESSAGE, item, name, arg, isAction);
    }

    // documentation inherited from RoomProvider
    public void sendSpriteSignal (ClientObject caller, String name, byte[] arg)
    {
        // Caller could be a WindowClientObject if coming from a thane client
        if (caller instanceof MemberObject) {
            // make sure the caller is in the room
            MemberObject who = (MemberObject)caller;
            if (!_roomObj.occupants.contains(who.getOid())) {
//                if (!isRecentOccupant(who.getOid())) {
//                    log.warning("Rejecting sprite signal request by non-occupant",
//                        "where", where(), "who", who.who(), "left", whenLeft(who.getOid()),
//                        "name", name);
//                }
                return;
            }
        }

        // dispatch this as a simple MessageEvent
        _roomObj.postMessage(RoomCodes.SPRITE_SIGNAL, name, arg);
    }

    // documentation inherited from RoomProvider
    public void setActorState (ClientObject caller, ItemIdent item, int actorOid, String state)
    {
        if (caller instanceof MemberObject) {
            MemberObject who = (MemberObject) caller;
            if (!_roomObj.occupants.contains(who.getOid())) {
//                if (!isRecentOccupant(who.getOid())) {
//                    log.warning("Rejecting actor state request by non-occupant", "where", where(),
//                        "who", who.who(), "left", whenLeft(who.getOid()), "item", item,
//                        "state", state);
//                }
                return;
            }
        }

        // make sure the actor to be state-changed is also in this room
        MsoyBodyObject actor;
        if (caller.getOid() != actorOid) {
            if (!_roomObj.occupants.contains(actorOid)) {
//                log.warning("Rejecting actor state request for non-occupant", "where", where(),
//                    "who", caller.who(), "left", whenLeft(actorOid), "item", item,
//                    "state", state);
                return;
            }
            actor = (MsoyBodyObject) _omgr.getObject(actorOid);

        } else {
            // the actor is the caller
            actor = (MemberObject)caller;
        }

        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        if (!ensureEntityControl(caller, item, "setState")) {
            log.info("Dropping change state for lack of control", "who", caller.who(),
                "item", item, "state", state);
            return;
        }

        // call the public (non-invocation service) method to enact it
        setState(actor, state);
    }

    // documentation inherited from RoomProvider
    public void editRoom (ClientObject caller, RoomService.ResultListener listener)
        throws InvocationException
    {
        requireManager(caller);

        // for now send back a TRUE
        listener.requestProcessed(Boolean.TRUE);
    }

    // documentation inherited from RoomProvider
    public void updateRoom (ClientObject caller, SceneUpdate update,
                            RoomService.InvocationListener listener)
        throws InvocationException
    {
        MemberObject user = requireManager(caller);
        doRoomUpdate(update, user.getMemberId(), user);
    }

    // from interface RoomProvider
    public void purchaseRoom (ClientObject caller, final RoomService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;

        // make sure they have editing privileges in this scene as they will be adding to this
        // scene group
        MsoyScene scene = (MsoyScene) _scene;
        if (!scene.canManage(user)) {
            throw new InvocationException(RoomCodes.E_ACCESS_DENIED);
        }
        MsoySceneModel model = (MsoySceneModel) scene.getSceneModel();

        // figure out if they want a group or a personal room
        boolean isGroup = (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP);
        final byte ownerType = isGroup ? MsoySceneModel.OWNER_TYPE_GROUP
                                       : MsoySceneModel.OWNER_TYPE_MEMBER;
        final int ownerId = isGroup ? model.ownerId : user.getMemberId();
        final String roomName = isGroup ? // TODO: i18n!
            "New 'somegroup' room" : (user.memberName + "'s new room");
        final String portalAction = scene.getId() + ":" + scene.getName();

        // TODO: charge some flow

        _invoker.postUnit(new RepositoryUnit("purchaseRoom") {
            public void invokePersist () throws Exception {
                _newRoomId = _sceneRepo.createBlankRoom(
                    ownerType, ownerId, roomName, portalAction, false);
            }
            public void handleSuccess () {
                listener.requestProcessed(_newRoomId);
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to create a new room", "user", user.which(),
                    "error", pe, "cause", pe.getCause());
                listener.requestFailed(RoomCodes.INTERNAL_ERROR);
            }
            protected int _newRoomId;
        });
    }

    // from interface RoomProvider
    public void publishRoom (ClientObject caller, RoomService.InvocationListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;
        if (!canManage(user)) {
            throw new InvocationException(RoomCodes.E_ACCESS_DENIED);
        }

        _invoker.postUnit(new WriteOnlyUnit("publishRoom") {
            public void invokePersist () {
                _sceneRepo.publishScene(_scene.getId());
            }
        });
    }

    // documentation inherited from RoomProvider
    public void updateMemory (
        ClientObject caller, final ItemIdent ident, String key, byte[] newValue,
        RoomService.ResultListener listener)
    {
        // TODO: Validate that the client is at least in the same room?

// NOTE: I've disabled the need to be in control to update memory (Ray July 6, 2007)
//        // if this client does not currently control this entity; ignore the request; if no one
//        // controls it, this will assign this client as controller
//        MemberObject who = (MemberObject) caller;
//        if (!checkAssignControl(who, entry.item, "updateMemory")) {
//            return;
//        }

        // TODO: verify that the caller is in the scene with this item, other item specific
        // restrictions

        // verify that the memory does not exceed legal size
        EntityMemories mems = _roomObj.memories.get(ident);
        int totalSize = (mems == null) ? 0 : mems.getSize(key);
        int newSize = EntityMemories.getSize(key, newValue);
        if ((totalSize + newSize) > EntityMemories.MAX_ENCODED_MEMORY_LENGTH) {
            log.info("Rejecting memory update as too large",
                "otherSize", totalSize, "newEntrySize", newSize);
            // Let the client know we looked at the memory, but didn't actually store it
            listener.requestProcessed(Boolean.FALSE);
            return;
        }

        // mark it as modified and update the room object; we'll save it when we unload the room
        _roomObj.updateMemory(ident, key, newValue);
        listener.requestProcessed(Boolean.TRUE);
    }

    // from interface RoomProvider
    public void changeLocation (ClientObject caller, ItemIdent item, Location newLoc)
    {
        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        if (!ensureEntityControl(caller, item, "changeLocation")) {
            return;
        }

        int oid = findActorOid(item);
        if (oid != 0) {
            _roomObj.updateOccupantLocs(new SceneLocation(newLoc, oid));
        }
    }

    // from RoomProvider
    public void spawnMob (
        ClientObject caller, int gameId, String mobId, String mobName, Location startLoc,
        final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        if (!WindowClientObject.isForGame(caller, gameId)) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }

        Tuple<Integer, String> key = new Tuple<Integer, String>(gameId, mobId);
        if (_mobs.containsKey(key)) {
            log.warning(
                "Tried to spawn mob that's already present", "gameId", gameId, "mobId", mobId);
            listener.requestFailed(RoomCodes.E_INTERNAL_ERROR);
            return;
        }

        if (StringUtil.isBlank(mobName)) {
            throw new IllegalArgumentException(
                "Mob spawn request without name [gameId=" + gameId + ", mobId=" + mobId + "]");
        }

        final MobObject mobObj = _omgr.registerObject(new MobObject());
        mobObj.setGameId(gameId);
        mobObj.setIdent(mobId);
        mobObj.setUsername(new Name(mobName));
        _mobs.put(key, mobObj);

        // prepare to set the starting location
        _startingLocs.put(mobObj.getOid(), startLoc);

        // then enter the place
        _locmgr.moveTo(mobObj, _plobj.getOid());
    }

    // from RoomProvider
    public void moveMob (
        ClientObject caller, int gameId, String mobId, Location newLoc,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        if (!WindowClientObject.isForGame(caller, gameId)) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }

        Tuple<Integer, String> key = new Tuple<Integer, String>(gameId, mobId);

        final MobObject mobObj = _mobs.get(key);
        if (mobObj == null) {
            log.warning(
                "Tried to move mob that's not present", "gameId", gameId, "mobId", mobId);
            listener.requestFailed(RoomCodes.E_INTERNAL_ERROR);
            return;
        }

        changeLocation(mobObj, (MsoyLocation)newLoc);
    }

    // from RoomProvider
    public void despawnMob (
        ClientObject caller, int gameId, String mobId,
        final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        if (!WindowClientObject.isForGame(caller, gameId)) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }

        Tuple<Integer, String> key = new Tuple<Integer, String>(gameId, mobId);

        final MobObject mobObj = _mobs.get(key);
        if (mobObj == null) {
            log.warning(
                "Tried to despawn mob that's not present", "gameId", gameId, "mobId", mobId);
            listener.requestFailed(RoomCodes.E_INTERNAL_ERROR);
            return;
        }

        _locmgr.leaveOccupiedPlace(mobObj);
        _omgr.destroyObject(mobObj.getOid());
        _mobs.remove(key);
    }

    // from RoomProvider
    public void setProperty (ClientObject caller, String propName, Object data, Integer key,
        boolean isArray, boolean testAndSet, Object testValue,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // This call is only allowed from a bureau window
        if (!(caller instanceof WindowClientObject)) {
            throw new InvocationException(RoomCodes.E_CANNOT_SET_PROPERTY);
        }

        // Fish out the game id
        String bureauId = ((WindowClientObject)caller).bureauId;
        if (!bureauId.startsWith(BureauTypes.GAME_BUREAU_ID_PREFIX)) {
            log.warning("Bad bureau id", "bureauId", bureauId, "caller", caller);
            throw new InvocationException(RoomCodes.E_CANNOT_SET_PROPERTY);
        }
        int gameId = Integer.parseInt(bureauId.substring(
            BureauTypes.GAME_BUREAU_ID_PREFIX.length()));

        // Find the properties
        RoomPropertiesEntry entry = _roomObj.propertySpaces.get(gameId);
        if (entry == null) {
            log.warning("Properties not loaded", "caller", caller);
            throw new InvocationException(RoomCodes.E_CANNOT_SET_PROPERTY);
        }
        RoomPropertiesObject props = (RoomPropertiesObject)_omgr.getObject(entry.propsOid);

        // Test, if requested
        if (testAndSet && !PropertySpaceHelper.testProperty(props, propName, testValue)) {
            return; // the test failed: do not set the property
        }

        // And apply
        try {
            Object oldData = PropertySpaceHelper.applyPropertySet(
                props, propName, data, key, isArray);
            props.postEvent(
                new PropertySetEvent(props.getOid(), propName, data, key, isArray, oldData));
        } catch (PropertySetException pse) {
            throw new InvocationException(
                MessageBundle.tcompose(RoomCodes.E_CANNOT_SET_PROPERTY, pse.getMessage()));
        }
    }

    // from RoomProvider
    public void rateRoom (ClientObject caller, final byte rating,
                          final RoomService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject) caller;

        if (member.isGuest()) {
            throw new InvocationException(RoomCodes.E_INTERNAL_ERROR);
        }

        _invoker.postUnit(new WriteOnlyUnit("rateRoom") {
            public void invokePersist () throws Exception {
                _sceneRepo.getRatingRepository().rate(getScene().getId(),
                    member.getMemberId(), rating);
            }
        });
    }

    // from RoomProvider
    public void sendPostcard (ClientObject caller, final String[] recips, final String subject,
                              final String caption, final String snapURL,
                              RoomService.ConfirmListener lner)
        throws InvocationException
    {
        final MemberObject sender = (MemberObject)caller;

        // sanity check
        if (recips.length > 25) {
            log.warning("ZOMG, rejecting spammer", "who", sender.who(), "recips", recips);
            throw new InvocationException(RoomCodes.E_INTERNAL_ERROR);
        }

        // if we have a snap URL, we can send the mail directly
        if (snapURL != null) {
            sendPostcardMail(sender, recips, subject, caption, snapURL);
            lner.requestProcessed();
            return;
        }

        // otherwise we need to look up the URL of the canonical scene snapshot
        _invoker.postUnit(new PersistingUnit(lner) {
            public void invokePersistent () throws Exception {
                // if we have no snapshot URL, we want the canonical URL
                SceneRecord srec = _sceneRepo.loadScene(getScene().getId());
                _snap = (srec == null) ? null : srec.getSnapshot();
                if (_snap == null) {
                    log.warning("Unable to load snapshot", "where", where(), "srec", srec);
                    throw new InvocationException(RoomCodes.E_INTERNAL_ERROR);
                }
            }
            public void handleSuccess () {
                sendPostcardMail(sender, recips, subject, caption, _snap.getMediaPath());
                super.handleSuccess();
            }
            protected MediaDesc _snap;
        });
    }

    @Override // from PlaceManager
    public void bodyWillEnter (BodyObject body)
    {
        // provide MsoyBodyObject instances with a RoomLocal they can use to determine stoniness
        // and managerness; MsoyBodyObject clears this local out in its didLeavePlace() override
        if (body instanceof MsoyBodyObject) {
            // add them to our list of ordered bodies
            _actors.add(body.getOid());

            body.setLocal(RoomLocal.class, new RoomLocal() {
                public boolean useStaticMedia (MsoyBodyObject body) {
                    return _actors.indexOf(body.getOid()) >= ACTOR_RENDERING_LIMIT;
                }
                public boolean isManager (MsoyBodyObject body) {
                    return (body instanceof MemberObject) && canManage((MemberObject)body);
                }
            });
        }

        if (body instanceof MemberObject) {
            // as we arrive at a room, we entrust it with our memories for broadcast to clients
            body.getLocal(MemberLocal.class).willEnter((MemberObject)body, _roomObj);
        }

        super.bodyWillEnter(body);
    }

    @Override // from PlaceManager
    public void bodyWillLeave (BodyObject body)
    {
        if (body instanceof MemberObject) {
            body.getLocal(MemberLocal.class).willLeave((MemberObject)body, _roomObj);
        }

        super.bodyWillLeave(body);
    }

    @Override // from SpotSceneManager
    public void willTraversePortal (BodyObject body, Portal portal)
    {
        MsoyLocation loc = (MsoyLocation) portal.getLocation();
        // We need to set the body's orientation to match the approach to the portal.
        // Look up their current location and move them from there. This could be a little
        // "off" if their sprite has not yet walked to this location, but oh well.
        SceneLocation sloc = _roomObj.occupantLocs.get(body.getOid());
        if (sloc != null) {
            MsoyLocation origin = (MsoyLocation) sloc.loc;
            double radians = Math.atan2(loc.z - origin.z, loc.x - origin.x);
            // turn the radians into a positive degree value in the whirled orientation space
            loc.orient = (short) ((360 + 90 + (int) Math.round(Math.toDegrees(radians))) % 360);
        }

        // note: we don't call super, we call updateLocation() ourselves
        updateLocation(body, loc);
    }

    @Override // from PlaceManager
    public void messageReceived (MessageEvent event)
    {
        // we want to explicitly disable the standard method calling by name that we allow in more
        // trusted environments
    }

    /**
     * Checks to see if an item is being controlled by any client. If not, the calling client is
     * assigned as the item's controller and true is returned. If the item is already being
     * controlled or is controllable by the calling client, true is returned. Otherwise false is
     * returned (indicating that another client currently has control of the item or the client
     * is not allowed to control the item).
     */
    protected boolean ensureEntityControl (ClientObject who, ItemIdent item, String from)
    {
        Integer memberOid = _avatarIdents.get(item);
        if (memberOid != null) {
            if (who instanceof WindowClientObject) {
                // Agents may control avatars that are playing their game
                MemberObject target = (MemberObject)_omgr.getObject(memberOid);
                if (target.game == null || !target.game.avrGame ||
                    !WindowClientObject.isForGame(who, target.game.gameId)) {
                    log.info("Agent attempting control of non-player avatar", "who",
                        who.who(), "avatar", item);
                    return false;
                }
                return true;

            } else if (who.getOid() == memberOid.intValue()) {
                // yes, you may control your own avatar
                return true;
            }
            log.warning("Some user is trying to control another's avatar", "who", who.who(),
                "avatar", item, "member", memberOid);
            return false;
        }
        // otherwise, it's for some entity other than a user's avatar...

        Controllable reference = new ControllableEntity(item);
        EntityControl ctrl = _roomObj.controllers.get(reference);
        if (ctrl == null) {
            //log.info("Assigning control", "item", item, "to", who.who());
            _roomObj.addToControllers(new EntityControl(reference, who.getOid()));
            return true;
        }
        return (ctrl.controllerOid == who.getOid());
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new RoomObject();
    }

    @Override // from PlaceManager
    protected long idleUnloadPeriod ()
    {
        return ROOM_IDLE_UNLOAD_PERIOD;
    }

    @Override // from SceneManager
    protected void gotSceneData (Object extras)
    {
        super.gotSceneData(extras);

        _extras = (RoomExtras) extras;
    }

    @Override
    protected AccessController getAccessController ()
    {
        return ROOM;
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // set up our room object
        _roomObj = (RoomObject) _plobj;
        _roomObj.setRoomService(_invmgr.registerDispatcher(new RoomDispatcher(this)));
        _roomObj.addListener(_roomListener);

        // register ourselves in our peer object
        MsoyScene mscene = (MsoyScene) _scene;
        _peerMan.roomDidStartup(mscene.getId(), mscene.getName(), mscene.getOwnerId(),
                                          mscene.getOwnerType(), mscene.getAccessControl());

        // if we have memories for the items in our room, add'em to the room object
        if (_extras.memories != null) {
            addMemoriesToRoom(_extras.memories);
        }

        // load up any pets that are "let out" in this room scene
        _petMan.loadRoomPets(_roomObj, _scene.getId());

        // we're done with our auxiliary scene information, let's let it garbage collect
        _extras = null;
    }

    @Override // from PlaceManager
    protected void bodyEntered (final int bodyOid)
    {
        super.bodyEntered(bodyOid);

        DObject body = _omgr.getObject(bodyOid);
        if (body instanceof MemberObject) {
            MemberObject member = (MemberObject) body;
            ensureAVRGamePropertySpace(member);

            // update some panopticon tracking that we'll log at the end of their session
            MsoySceneModel model = (MsoySceneModel) getScene().getSceneModel();
            boolean isMemberScene = (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER);
            member.getLocal(MemberLocal.class).metrics.room.init(isMemberScene, model.ownerId);

            // update stats for badge/passport reasons
            if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                member.getLocal(MemberLocal.class).stats.addToSetStat(
                    StatType.WHIRLEDS_VISITED, model.ownerId);
            }

            // log this room entry to panopticon for future grindery
            boolean isWhirled = (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP);
            _eventLog.roomEntered(member.getMemberId(), isWhirled, member.getVisitorId());

            // update this user's experiences re: visiting this room (unless it's their home)
            if (member.homeSceneId != model.sceneId) {
                _memberMan.addExperience(member,
                    new MemberExperience(new Date(), HomePageItem.ACTION_ROOM, model.sceneId));
            }
        }
    }

    @Override // from PlaceManager
    protected void bodyUpdated (OccupantInfo info)
    {
        super.bodyUpdated(info);

        // if this occupant just disconnected, reassign their controlled entities
        if (info.status == OccupantInfo.DISCONNECTED) {
            reassignControllers(info.bodyOid);
        }
    }

    @Override // from PlaceManager
    protected void bodyLeft (int bodyOid)
    {
        // start metrics
        DObject body = _omgr.getObject(bodyOid);
        if (body instanceof MemberObject) {
            MemberObject member = (MemberObject)body;
            if (!member.isViewer()) {
                member.getLocal(MemberLocal.class).metrics.room.save(member);

                // get the last known occupancy length - this might have been measured above,
                // or by the peer serialization code if we're moving across servers
                int secondsInRoom =
                    member.getLocal(MemberLocal.class).metrics.room.getLastOccupancyLength();
                MsoySceneModel model = (MsoySceneModel)getScene().getSceneModel();
                boolean isWhirled = (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP);
                _eventLog.roomLeft(member.getMemberId(), model.sceneId, isWhirled,
                                   secondsInRoom, _roomObj.occupants.size(), member.getVisitorId());
            }
        }

        super.bodyLeft(bodyOid);

        // reassign this occupant's controlled entities
        reassignControllers(bodyOid);

        // remove this body from our actor list
        _actors.remove((Object)bodyOid); // force usage of the Object, we're not removing by index

        // potentially unstone one or more actors
        for (int ii = 0, ll = _actors.size(); ii < ll; ii++) {
            boolean shouldBeStatic = (ii >= ACTOR_RENDERING_LIMIT);
            ActorInfo info = (ActorInfo)_plobj.occupantInfo.get(_actors.get(ii));
            if (info != null && info.isStatic() != shouldBeStatic) {
                final MsoyBodyObject abody = (MsoyBodyObject)_omgr.getObject(info.bodyOid);
                if (abody != null) {
                    updateOccupantInfo(bodyOid, new ActorInfo.Updater<ActorInfo>() {
                        public boolean update (ActorInfo info) {
                            info.updateMedia(abody);
                            return true;
                        }
                    });
                }
            }
        }

//        // purge any body oids that left more than a few seconds ago
//        int now = (int)(System.currentTimeMillis() / 1000);
//        int limit = now - LEFT_BODY_PURGE_SECS;
//        for (Interator vals = _left.values(); vals.hasNext(); ) {
//            if (vals.nextInt() < limit) {
//                vals.remove();
//            }
//        }
//
//        // mark this oid as having just left
//        _left.put(bodyOid, now);
    }

    @Override // from PlaceManager
    protected boolean shouldDeclareEmpty (OccupantInfo leaver)
    {
        for (OccupantInfo info : _plobj.occupantInfo) {
            if (info instanceof MemberInfo || info instanceof ObserverInfo) {
                return false; // there's a human still here!
            }
        }
        return true;
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        _roomObj.removeListener(_roomListener);
        _invmgr.clearDispatcher(_roomObj.roomService);

        super.didShutdown();

        // clear out our peer hosting information
        _peerMan.roomDidShutdown(_scene.getId());

        // shut our pets down
        _petMan.shutdownRoomPets(_roomObj);

        // flush any modified memory records to the database
        flushMemories(_invoker, _memoryRepo, _roomObj.memories);

        // flush modified property spaces and destroy dobjects
        for (RoomPropertiesEntry entry : _roomObj.propertySpaces) {
            RoomPropertiesObject properties = (RoomPropertiesObject)_omgr.getObject(entry.propsOid);
            flushAVRGamePropertySpace(entry.ownerId, properties);
            _omgr.destroyObject(entry.propsOid);
        }
    }

    /**
     * If the given member is playing an AVRG, make sure the {@link RoomObject#propertySpaces}
     * contains an entry for its game. If the new object is created, load persistent properties
     * from the database.
     */
    protected void ensureAVRGamePropertySpace (MemberObject member)
    {
        if (member.game == null || !member.game.avrGame) {
            return;
        }

        final int gameId = member.game.gameId;
        if (_roomObj.propertySpaces.containsKey(gameId)) {
            return;
        }

        if (_pendingGameIds.contains(gameId)) {
            log.warning("Room property resolution already pending", "gameId", gameId,
                        "sceneId", _scene.getId(), "memberId", member.getMemberId());
            return;
        }

        final RoomPropertiesObject props = new RoomPropertiesObject();
        final PropertySpaceHandler propertyService = new PropertySpaceHandler(props) {
            @Override protected void validateUser (ClientObject caller)
                throws InvocationException {
                if (!WindowClientObject.isForGame(caller, gameId)) {
                    throw new InvocationException(InvocationCodes.ACCESS_DENIED);
                }
            }};
        final WhirledGameMessageHandler messageService =  new WhirledGameMessageHandler(props) {
            @Override protected ClientObject getAudienceMember (int id)
                throws InvocationException {
                // We don't support private messages in rooms (the client should use
                // AVRGameObject.messageService instead)
                throw new InvocationException(InvocationCodes.ACCESS_DENIED);
            }

            @Override protected boolean isAgent (ClientObject caller) {
                return WindowClientObject.isForGame(caller, gameId);
            }

            @Override protected void validateSender (ClientObject caller)
                throws InvocationException {
                // Allow agents and players
                if (caller instanceof WindowClientObject) {
                    if (isAgent(caller)) {
                        return;
                    }
                } else if (caller instanceof MemberObject) {
                    if (((MemberObject)caller).game.gameId == gameId) {
                        return;
                    }
                }
                throw new InvocationException(InvocationCodes.ACCESS_DENIED);
            }

            @Override protected int resolvePlayerId (ClientObject caller) {
                // Always use member id for avrgs
                return ((MemberObject)caller).getMemberId();
            }};

        _pendingGameIds.add(gameId);
        _invoker.postUnit(new Invoker.Unit("loadProps") {
            public boolean invoke () {
                try {
                    _propRecs = _sceneRepo.loadProperties(gameId, _scene.getId());
                } catch (Exception e) {
                    log.warning("Failed to load room properties", "where", where(),
                                "gameId", gameId, e);
                }
                return true;
            }

            public void handleResult () {
                // Create map of loaded properties
                Map<String, byte[]> propRecsMap = Maps.newHashMap();
                if (_propRecs != null) {
                    for (RoomPropertyRecord propRec : _propRecs) {
                        propRecsMap.put(propRec.name, propRec.value);
                    }
                }

                // Create the dobj
                _omgr.registerObject(props);

                // Populate
                PropertySpaceHelper.initWithProperties(
                    props, PropertySpaceHelper.recordsToProperties(propRecsMap));

                // Set members
                props.setPropertiesService(
                    _invmgr.registerDispatcher(new PropertySpaceDispatcher(propertyService)));
                props.setMessageService(
                    _invmgr.registerDispatcher(new WhirledGameMessageDispatcher(messageService)));

                // Add to room
                RoomPropertiesEntry entry = new RoomPropertiesEntry();
                entry.ownerId = gameId;
                entry.propsOid = props.getOid();
                _roomObj.addToPropertySpaces(entry);

                // Clear from pending
                _pendingGameIds.remove(gameId);

                log.info("Added property space", "roomOid", _roomObj.getOid(), "gameId", gameId,
                    "sceneId", _scene.getId(), "propsOid", props.getOid());
            }

            Collection<RoomPropertyRecord> _propRecs;
        });
    }

    /**
     * Write changed room properties to the database.
     */
    protected void flushAVRGamePropertySpace  (final int ownerId, RoomPropertiesObject properties)
    {
        log.info("Flushing avrg room properties", "roomOid", _roomObj.getOid(),
            "propsOid", properties.getOid());

        final Map<String, byte[]> encodedMap =
            PropertySpaceHelper.encodeDirtyStateForStore(properties);
        final int sceneId = _scene.getId();
        _invoker.postUnit(new WriteOnlyUnit("saveRoomProps") {
            public void invokePersist() throws Exception {
                for (Map.Entry<String, byte[]> entry : encodedMap.entrySet()) {
                    _sceneRepo.storeProperty(new RoomPropertyRecord(
                        ownerId, sceneId, entry.getKey(), entry.getValue()));
                }
            }});

        _invmgr.clearDispatcher(properties.propertiesService);
        _invmgr.clearDispatcher(properties.messageService);
    }

    @Override // documentation inherited
    protected SceneLocation computeEnteringLocation (BodyObject body, Portal from, Portal entry)
    {
//        if (body instanceof MemberObject) {
//            // automatically add the room to their recent list
//            MemberObject memberObj = (MemberObject) body;
//            memberObj.addToRecentScenes(_scene.getId(), _scene.getName());
//        }

        // If we have explicitly set the starting location for some reason, use that
        Location loc = _startingLocs.remove(body.getOid());
        if (loc != null) {
            return new SceneLocation(loc, body.getOid());
        }

        // if the from portal has a destination location, use that
        if (from instanceof MsoyPortal && ((MsoyPortal)from).dest != null) {
            return new SceneLocation(((MsoyPortal)from).dest, body.getOid());
        }

        // otherwise if we have a destination portal (the scene's default entrace) use that
        if (entry != null) {
            return super.computeEnteringLocation(body, from, entry);
        }

        // fallback if there is no portal
        return new SceneLocation(new MsoyLocation(.5, 0, .5, (short) 0), body.getOid());
    }

    /**
     * Performs the given updates.
     * @param user may be null if unavailable.
     */
    protected void doRoomUpdate (
        final SceneUpdate update, final int memberId, final MemberObject user)
    {
        // TODO: complicated verification of changes, including verifying that the user owns any
        // item they're adding, etc.

        Runnable doUpdateScene = new Runnable() {
            public void run () {
                // initialize and record this update to the scene management system (which will
                // persist it, send it to the client for application to the scene, etc.)
                update.init(_scene.getId(), _scene.getVersion());
                recordUpdate(update);

                // let the registry know that rooms be gettin' updated (TODO: don't do this on
                // every fucking update, it's super expensive)
                if (user != null) {
                    ((MsoySceneRegistry)_screg).memberUpdatedRoom(user, (MsoyScene)_scene);
                }
            }
        };

        if (update instanceof SceneAttrsUpdate) {
            SceneAttrsUpdate up = (SceneAttrsUpdate) update;
            MsoyScene msoyScene = (MsoyScene) _scene;

            // massage the room name to make sure it's kosher
            up.name = StringUtil.truncate(up.name, MsoySceneModel.MAX_NAME_LENGTH);

            // if decor was modified, we should mark new decor as used, and clear the old one
            Decor decor = msoyScene.getDecor();
            if (decor != null && decor.itemId != up.decor.itemId) { // modified?
                _itemMan.updateItemUsage(
                    Item.DECOR, Item.USED_AS_BACKGROUND, memberId, _scene.getId(),
                    decor.itemId, up.decor.itemId, new ComplainingListener<Object>(
                        log, "Unable to update decor usage"));
            }

            // same with background audio - mark new one as used, unmark old one
            AudioData audioData = msoyScene.getAudioData();
            int curAudioId = (audioData == null) ? 0 : audioData.itemId;
            int newAudioId = (up.audioData == null) ? 0 : up.audioData.itemId;
            if (curAudioId != newAudioId) {
                _itemMan.updateItemUsage(
                    Item.AUDIO, Item.USED_AS_BACKGROUND, memberId, _scene.getId(),
                    curAudioId, newAudioId,
                    new ComplainingListener<Object>(log, "Unable to update audio usage"));
            }

            // if the name or access controls were modified, we need to update our HostedPlace
            boolean nameChange = !msoyScene.getName().equals(up.name);
            if (nameChange || msoyScene.getAccessControl() != up.accessControl) {
                _peerMan.roomUpdated(msoyScene.getId(), up.name,
                    msoyScene.getOwnerId(), msoyScene.getOwnerType(),
                    up.accessControl);
            }
        }

        // furniture modification updates require us to mark item usage
        if (update instanceof FurniUpdate.Remove) {
            // mark this item as no longer in use
            FurniData data = ((FurniUpdate)update).data;
            _itemMan.updateItemUsage(
                data.itemType, Item.UNUSED, memberId, _scene.getId(),
                data.itemId, 0, new ComplainingListener<Object>(
                    log, "Unable to clear furni item usage"));

            // clear out any memories that were loaded for this item
            if (data.itemType != Item.NOT_A_TYPE) {
                removeAndFlushMemories(data.getItemIdent());
            }

        } else if (update instanceof FurniUpdate.Add) {
            // mark this item as in use
            FurniData data = ((FurniUpdate)update).data;
            _itemMan.updateItemUsage(
                data.itemType, Item.USED_AS_FURNITURE, memberId, _scene.getId(),
                0, data.itemId, new ComplainingListener<Object>(
                    log, "Unable to set furni item usage"));

            // and resolve any memories it may have, calling the scene updater when it's done
            resolveMemories(Collections.singleton(data.getItemIdent()), doUpdateScene);
            // don't fall through here
            return;
        }

        doUpdateScene.run();
    }

    /**
     * Determine the actor oid that corresponds to the specified ItemIdent, or return 0 if none
     * found.
     */
    protected int findActorOid (ItemIdent item)
    {
        // see if it's an avatar
        Integer oid = _avatarIdents.get(item);
        if (oid != null) {
            return oid.intValue();
        }

        // otherwise, scan all occupant infos. Perhaps we should keep a mapping for non-avatar
        // actors as well?
        for (OccupantInfo info : _roomObj.occupantInfo) {
            if (info instanceof ActorInfo) {
                ActorInfo ainfo = (ActorInfo)info;
                if (ainfo.getItemIdent().equals(item)) {
                    return ainfo.getBodyOid();
                }
            }
        }

        return 0; // never found it..
    }

    /**
     * Reassigns all scene entities controlled by the specified client to new controllers.
     */
    protected void reassignControllers (int bodyOid)
    {
        // determine which items were under the control of this user
        List<Controllable> items = Lists.newArrayList();
        for (EntityControl ctrl : _roomObj.controllers) {
            if (ctrl.controllerOid == bodyOid) {
                items.add(ctrl.controlled);
            }
        }
        if (items.size() == 0) {
            return;
        }

        // clear out the old controller mappings
        try {
            _roomObj.startTransaction();
            for (Controllable item : items) {
                _roomObj.removeFromControllers(item);
            }
        } finally {
            _roomObj.commitTransaction();
        }

        // assign new mappings to remaining users
        assignControllers(items);
    }

    /**
     * Handles a request to select a controller for the supplied set of items.
     */
    protected boolean assignControllers (Collection<Controllable> ctrlables)
    {
        // determine the available controllers
        HashIntMap<Controller> controllers = new HashIntMap<Controller>();
        for (OccupantInfo info : _roomObj.occupantInfo) {
            if (info instanceof MemberInfo && info.status != OccupantInfo.DISCONNECTED) {
                controllers.put(info.bodyOid, new Controller(info.bodyOid));
            }
        }

        // if we have no potential controllers, the controllables will remain uncontrolled (which
        // is much better than them being out of control :)
        if (controllers.size() == 0) {
            return false;
        }

        // note the current load of these controllers
        for (EntityControl ctrl : _roomObj.controllers) {
            Controller owner = controllers.get(ctrl.controllerOid);
            if (owner != null) {
                owner.load++;
            }
        }

        // choose the least loaded controller that is compatible with the controllable, remove the
        // controller from the set, assign them control of the controllable, add them back to the
        // set, then finally move to the next item
        try {
            _roomObj.startTransaction();
            TreeSet<Controller> set = new TreeSet<Controller>(controllers.values());
            for (Controllable ctrlable : ctrlables) {
                for (Controller ctrl : set) {
                    MemberObject mobj = (MemberObject)_omgr.getObject(ctrl.bodyOid);
                    if (mobj == null || !ctrlable.isControllableBy(mobj)) {
                        continue;
                    }
                    set.remove(ctrl);
                    ctrl.load++;
                    //log.info("Assigning control", "item", ctrlable, "to", ctrl.bodyOid);
                    _roomObj.addToControllers(new EntityControl(ctrlable, ctrl.bodyOid));
                    set.add(ctrl);
                    break;
                }
            }

        } finally {
            _roomObj.commitTransaction();
        }
        return true;
    }

    /**
     * Loads up all specified memories and places them into the room object.
     */
    protected void resolveMemories (final Collection<ItemIdent> idents, final Runnable onSuccess)
    {
        _invoker.postUnit(new RepositoryUnit("resolveMemories") {
            public void invokePersist () throws Exception {
                _mems = _memoryRepo.loadMemories(idents);
            }
            public void handleSuccess () {
                addMemoriesToRoom(_mems);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
            protected Collection<MemoriesRecord> _mems;
        });
    }

    protected void addMemoriesToRoom (Collection<MemoriesRecord> memories)
    {
        _roomObj.startTransaction();
        try {
            for (MemoriesRecord mrec : memories) {
                _roomObj.putMemories(mrec.toEntry());
            }
        } finally {
            _roomObj.commitTransaction();
        }
    }

    protected void removeAndFlushMemories (ItemIdent item)
    {
        EntityMemories removed = _roomObj.takeMemories(item);
        if (removed != null) {
            // persist any of the old memories that were modified
            flushMemories(_invoker, _memoryRepo, Collections.singleton(removed));
        }
    }

    /**
     * Helper function for {@link #sendPostcard}.
     */
    protected void sendPostcardMail (MemberObject sender, String[] recips, String subject,
                                     String caption, String snapURL)
    {
        String memmail = sender.username.toString();
        for (String recip : recips) {
            _mailer.sendTemplateEmail(recip, memmail, "postcard", "sender", sender.memberName,
                                      "sender_email", memmail, "sender_id", sender.getMemberId(),
                                      "subject", subject, "caption", caption, "snap_url", snapURL,
                                      "title", getScene().getName(), "scene_id", getScene().getId(),
                                      "server_url", DeploymentConfig.serverURL);
        }
    }

//    protected String whenLeft (int bodyOid)
//    {
//        if (!_left.contains(bodyOid)) {
//            return "unknown";
//        }
//        int when = _left.get(bodyOid);
//        int now = (int)(System.currentTimeMillis() / 1000);
//        return "" + (now - when) + " seconds ago";
//    }
//
//    protected boolean isRecentOccupant (int bodyOid)
//    {
//        int whenLeft = _left.get(bodyOid);
//        int now = (int)(System.currentTimeMillis() / 1000);
//        return whenLeft > 0 && (now - whenLeft) < LEFT_BODY_PURGE_SECS;
//    }

    /** Listens to the room. */
    protected class RoomListener
        implements SetListener<OccupantInfo>
    {
        // from SetListener
        public void entryAdded (EntryAddedEvent<OccupantInfo> event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent(null, event.getEntry());
            }
        }

        // from SetListener
        public void entryUpdated (EntryUpdatedEvent<OccupantInfo> event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                if (event.getEntry() instanceof MemberInfo) {
                    MemberInfo entry = (MemberInfo)event.getEntry();
                    MemberInfo oldEntry = (MemberInfo)event.getOldEntry();

                    // see if they actually switched avatars
                    if (!entry.getItemIdent().equals(oldEntry.getItemIdent())) {
                        updateAvatarIdent(oldEntry, entry);
                        removeAndFlushMemories(oldEntry.getItemIdent());
                    }
                }
            }
        }

        // from SetListener
        public void entryRemoved (EntryRemovedEvent<OccupantInfo> event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent(event.getOldEntry(), null);
            }
        }

        /**
         * Maintain a mapping of ItemIdent -> oid for all MemberInfos.
         */
        protected void updateAvatarIdent (OccupantInfo oldInfo, OccupantInfo newInfo)
        {
            // we only track MemberInfo, as those are the only things that represent MemberObjects
            if (oldInfo instanceof MemberInfo) {
                _avatarIdents.remove(((MemberInfo)oldInfo).getItemIdent());
            }
            if (newInfo instanceof MemberInfo) {
                _avatarIdents.put(((MemberInfo)newInfo).getItemIdent(), newInfo.bodyOid);
            }
        }
    }

    /** Used during the process of controller assignment. */
    protected static class Controller implements Comparable<Controller>
    {
        public final int bodyOid;
        public int load;

        public Controller (int bodyOid) {
            this.bodyOid = bodyOid;
        }
        public boolean equals (Object other) {
            if (other instanceof Controller) {
                Controller that = (Controller) other;
                return (this.bodyOid == that.bodyOid);
            } else {
                return false;
            }
        }
        public int hashCode () {
            return bodyOid;
        }
        public int compareTo (Controller other) {
            // sort first by load, then by body oid
            int diff = Comparators.compare(load, other.load);
            if (diff == 0) {
                diff = Comparators.compare(bodyOid, other.bodyOid);
            }
            return diff;
        }
    } // End: static class Controller

    /** The room object. */
    protected RoomObject _roomObj;

    /** Extra data from scene resolution. */
    protected RoomExtras _extras;

    /** Game ids of properties we are currently loading. */
    protected ArrayIntSet _pendingGameIds = new ArrayIntSet();

    /** If non-null, a list of memberId blocked from the room. */
    protected ArrayIntSet _booted;

    /** Listens to the room object. */
    protected RoomListener _roomListener = new RoomListener();

    /** A list of the body oids of all actors in the room, ordered by when they entered. */
    protected List<Integer> _actors = Lists.newArrayList();

    /** Mapping to keep track of spawned mobs. */
    protected Map<Tuple<Integer, String>, MobObject> _mobs = Maps.newHashMap();

    /** Mapping to keep track of starting location of added bodies. */
    protected HashIntMap<Location> _startingLocs = new HashIntMap<Location>();

    /** For all MemberInfo's, a mapping of ItemIdent to the member's oid. */
    protected Map<ItemIdent, Integer> _avatarIdents = Maps.newHashMap();

//    /** Map of body oids that have left the room to the time they left (in seconds). */
//    protected IntIntMap _left = new IntIntMap();

    /** After this level of occupancy is reached, actors are made static. */
    protected static final int ACTOR_RENDERING_LIMIT = 20;

    /** Time to keep left body oids in {@link #_left}. */
    protected static final int LEFT_BODY_PURGE_SECS = 15;

    /**
     * We allow access as in {@link CrowdObjectAccess#PLACE} but also give full subscription
     * powers to {@link WindowClientObject} instances; these are the world server representatives
     * of server-side agents. We need this for AVRGs to be able to access room data.
     */
    protected static AccessController ROOM = new AccessController()
    {
        // documentation inherited from interface
        public boolean allowSubscribe (DObject object, Subscriber<?> sub)
        {
            if (sub instanceof ProxySubscriber) {
                ClientObject co = ((ProxySubscriber)sub).getClientObject();
                if (co instanceof WindowClientObject) {
                    return true;
                }
            }
            return CrowdObjectAccess.PLACE.allowSubscribe(object, sub);
        }

        // documentation inherited from interface
        public boolean allowDispatch (DObject object, DEvent event)
        {
            return CrowdObjectAccess.PLACE.allowDispatch(object, event);
        }
    };


    @Inject protected MailSender _mailer;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected PetManager _petMan;
    @Inject protected SceneRegistry _screg;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected MemberLocator _locator;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected LocationManager _locmgr;
    @Inject protected MemberManager _memberMan;
}
