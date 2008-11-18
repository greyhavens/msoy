//
// $Id$

package com.threerings.msoy.room.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.server.LocationManager;

import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.server.SpotSceneManager;

//import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.BootablePlaceManager;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.RatingRepository;

import com.threerings.msoy.bureau.data.WindowClientObject;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemManager;

import com.threerings.msoy.room.client.RoomService;
import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.AudioData;
import com.threerings.msoy.room.data.Controllable;
import com.threerings.msoy.room.data.ControllableAVRGame;
import com.threerings.msoy.room.data.ControllableEntity;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.EntityMemoryEntry;
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
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesEntry;
import com.threerings.msoy.room.data.RoomPropertiesObject;
import com.threerings.msoy.room.data.SceneAttrsUpdate;
import com.threerings.msoy.room.server.persist.MemoryRecord;
import com.threerings.msoy.room.server.persist.MemoryRepository;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.RoomPropertyRecord;

import com.whirled.bureau.data.BureauTypes;
import com.whirled.game.data.PropertySetEvent;
import com.whirled.game.data.PropertySpaceObject.PropertySetException;
import com.whirled.game.server.PropertySpaceDispatcher;
import com.whirled.game.server.PropertySpaceHandler;
import com.whirled.game.server.PropertySpaceHelper;
import com.whirled.game.server.WhirledGameMessageDispatcher;
import com.whirled.game.server.WhirledGameMessageHandler;

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
                                      Iterable<EntityMemoryEntry> entries)
    {
        final List<MemoryRecord> memrecs = MemoryRecord.extractModified(entries);
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
    public void setState (MsoyBodyObject actor, String state)
    {
        // update the state in their body object
        actor.actorState = state;

        // update the occupant info
        OccupantInfo occInfo = getOccupantInfo(actor.getOid());
        if (occInfo == null) {
            return; // if they're gone, no problem
        }
        if (!(occInfo instanceof ActorInfo)) {
            log.warning("Cannot update state for non-actor: " + occInfo);
            return;
        }

        ActorInfo winfo = (ActorInfo) occInfo;
        if (ObjectUtil.equals(winfo.getState(), state)) {
            return; // if there was no change, we're done.
        }

        // TODO: consider, instead of updating the whole dang occInfo, dispatching a custom event
        // that will update just the state and serve as the trigger event to usercode...
        winfo.setState(state);
        updateOccupantInfo(winfo);
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
     * Checks whether or not the calling user can bring pets into this room. Returns normally if
     * so, throws an {@link InvocationException} if not.
     */
    public void checkCanAddPet (MemberObject caller)
        throws InvocationException
    {
        if (!canManage(caller)) {
            throw new InvocationException(RoomCodes.E_CANNOT_ADD_PET);
        }
    }

    /**
     * Reclaims an item from this room.
     */
    public void reclaimItem (ItemIdent item, MemberObject user)
    {
        for (FurniData furni : ((MsoyScene)_scene).getFurni()) {
            if (item.equals(furni.getItemIdent())) {
                FurniUpdate.Remove update = new FurniUpdate.Remove();
                update.data = furni;
                doRoomUpdate(update, user);
                break;
            }
        }
    }

    /**
     * Reclaims this room's decor.
     */
    public void reclaimDecor (MemberObject user)
    {
        // replace the decor with defaults
        MsoyScene scene = (MsoyScene)_scene;
        SceneAttrsUpdate update = new SceneAttrsUpdate();
        update.init(scene.getId(), scene.getVersion());
        update.name = scene.getName();
        update.decor = MsoySceneModel.defaultMsoySceneModelDecor();
        update.audioData = scene.getAudioData();
        update.entrance = ((MsoySceneModel)scene.getSceneModel()).entrance;
        doRoomUpdate(update, user);
    }

    /**
     * Reclaim this room's background audio.
     */
    public void reclaimAudio (MemberObject user)
    {
        MsoyScene scene = (MsoyScene)_scene;
        AudioData ad = scene.getAudioData();
        ad.itemId = 0;
        SceneAttrsUpdate update = new SceneAttrsUpdate();
        update.init(scene.getId(), scene.getVersion());
        update.name = scene.getName();
        update.decor = scene.getDecor();
        update.audioData = ad;
        update.entrance = ((MsoySceneModel)scene.getSceneModel()).entrance;
        doRoomUpdate(update, user);
    }

    public void occupantLeftAVRGame (MemberObject member)
    {
        reassignControllers(member.getOid(), true);
    }

    public void occupantEnteredAVRGame (MemberObject member)
    {
        ensureAVRGameControl(member);
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
                log.warning("Rejecting sprite message request by non-occupant", "who", who.who(),
                    "item", item, "name", name);
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
                log.warning("Rejecting sprite signal request by non-occupant [who=" + who.who() +
                            ", name=" + name + "].");
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
                log.warning("Rejecting actor state request by non-occupant", "who", who.who(),
                    "item", item, "state", state);
                return;
            }
        }

        // make sure the actor to be state-changed is also in this room
        MsoyBodyObject actor;
        if (caller.getOid() != actorOid) {
            if (!_roomObj.occupants.contains(actorOid)) {
                log.warning("Rejecting actor state request for non-occupant", "who", caller.who(),
                    "item", item, "state", state);
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
        if (!canManage((MemberObject) caller)) {
            throw new InvocationException(RoomCodes.E_ACCESS_DENIED);
        }

        // for now send back a TRUE
        listener.requestProcessed(Boolean.TRUE);
    }

    // documentation inherited from RoomProvider
    public void updateRoom (ClientObject caller, SceneUpdate update,
                            RoomService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        if (!canManage(user)) {
            throw new InvocationException(RoomCodes.E_ACCESS_DENIED);
        }
        doRoomUpdate(update, user);
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
//                user.addToOwnedScenes(new SceneBookmarkEntry(_newRoomId, roomName, 0));
                listener.requestProcessed(_newRoomId);
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to create a new room [user=" + user.which() +
                            ", error=" + pe + ", cause=" + pe.getCause() + "].");
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
        ((MsoySceneRegistry)_screg).memberPublishedRoom(user, (MsoyScene)_scene);
    }

    // documentation inherited from RoomProvider
    public void updateMemory (
        ClientObject caller, final EntityMemoryEntry entry, RoomService.ResultListener listener)
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
        int totalSize = 0;
        for (EntityMemoryEntry rent : _roomObj.memories) {
            if (rent.item.equals(entry.item) && !rent.key.equals(entry.key)) {
                totalSize += rent.getSize();
            }
        }
        if (totalSize + entry.getSize() > EntityMemoryEntry.MAX_ENCODED_MEMORY_LENGTH) {
            log.info("Rejecting memory update as too large [otherSize=" + totalSize +
                     ", newEntrySize=" + entry.getSize() + "].");
            // Let the client know we looked at the memory, but didn't actually store it
            listener.requestProcessed(Boolean.FALSE);
            return;
        }

        // mark it as modified and update the room object; we'll save it when we unload the room
        entry.modified = true;
        if (_roomObj.memories.contains(entry)) {
            _roomObj.updateMemories(entry);
        } else {
            _roomObj.addToMemories(entry);
        }
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
        final InvocationListener listener)
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
        ClientObject caller, int gameId, String mobId, Location newLoc, InvocationListener listener)
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
    public void despawnMob (ClientObject caller, int gameId, String mobId,
                            final InvocationListener listener)
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
    public void rateRoom (ClientObject caller, byte rating, RoomService.InvocationListener listener)
        throws InvocationException
    {
        MemberObject member = (MemberObject) caller;

        if (member.isGuest()) {
            throw new InvocationException(RoomCodes.E_INTERNAL_ERROR);
        }

        Tuple<RatingRepository.RatingAverageRecord, Boolean> result =
            _sceneRepo.getRatingRepository().rate(getScene().getSceneModel().sceneId,
            member.getMemberId(), rating);

        _roomObj.startTransaction();
        _roomObj.setRating(result.left.average);
        _roomObj.setRatingCount(result.left.count);
        _roomObj.commitTransaction();
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

    @Override
    public void updateOccupantInfo (OccupantInfo occInfo)
    {
        // Prior to inserting and cloning, make sure we enforce render limits
        if (occInfo instanceof ActorInfo) {
            OccupantInfo prior = _occInfo.get(occInfo.getBodyOid());
            // Set to static if it was static before and the room is still crowded
            if (prior != null && prior instanceof ActorInfo && ((ActorInfo)prior).isStatic() &&
                _dynamicActors.size() > ACTOR_RENDERING_LIMIT) {
                ((ActorInfo)occInfo).useStaticMedia();
            }
        }
        super.updateOccupantInfo(occInfo);
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
            log.info("Assigning control [item=" + item + ", to=" + who.who() + "].");
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

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // set up our room object
        _roomObj = (RoomObject) _plobj;
        _roomObj.setRoomService(_invmgr.registerDispatcher(new RoomDispatcher(this)));
        _roomObj.addListener(_roomListener);

        RatingRepository.RatingAverageRecord rar =
            _sceneRepo.getRatingRepository().createAverageRecord(
                getScene().getSceneModel().sceneId);
        _roomObj.setRating(rar.average);
        _roomObj.setRatingCount(rar.count);

        // register ourselves in our peer object
        MsoyScene mscene = (MsoyScene) _scene;
        _peerMan.roomDidStartup(mscene.getId(), mscene.getName(), mscene.getOwnerId(),
                                          mscene.getOwnerType(), mscene.getAccessControl());

        // determine which (if any) items in this room have memories and load them up
        List<ItemIdent> memoryIds = Lists.newArrayList();
        for (FurniData furni : ((MsoyScene) _scene).getFurni()) {
            if (furni.itemType != Item.NOT_A_TYPE) {
                memoryIds.add(furni.getItemIdent());
            }
        }

        // set up
        Runnable doLoadPets = new Runnable() {
            public void run () {
                // load up any pets that are "let out" in this room scene
                _petMan.loadRoomPets(_roomObj, _scene.getId());
            }
        };

        if (memoryIds.size() > 0) {
            // if we have memories to resolve, do so before we load pets from the DB
            resolveMemories(memoryIds, doLoadPets);
        } else {
            // else do it now
            doLoadPets.run();
        }
    }

    @Override // from PlaceManager
    protected void bodyEntered (final int bodyOid)
    {
        DObject body = _omgr.getObject(bodyOid);
        if (body instanceof MemberObject) {
            final MemberObject member = (MemberObject) body;
            ensureAVRGameControl(member);
            ensureAVRGamePropertySpace(member);
            final MsoySceneModel model = (MsoySceneModel) getScene().getSceneModel();
            boolean isMemberScene = (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER);
            member.getLocal(MemberLocal.class).metrics.room.init(isMemberScene, model.ownerId);

            if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                member.getLocal(MemberLocal.class).stats.addToSetStat(
                    StatType.WHIRLEDS_VISITED, model.ownerId);
            }

            // log it!
            boolean isWhirled = (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP);
            _eventLog.roomEntered(member.getMemberId(), isWhirled, member.getVisitorId());

            // Indicate the user visited the room (unless it's their home).
            if (member.homeSceneId != model.sceneId) {
                _memberMan.addExperience(member, new MemberExperience(
                                             new Date(), HomePageItem.ACTION_ROOM, model.sceneId));
            }

            addMemoriesToRoom(member);
        }

        super.bodyEntered(bodyOid);
    }

    @Override // from PlaceManager
    protected void bodyUpdated (OccupantInfo info)
    {
        super.bodyUpdated(info);

        // if this occupant just disconnected, reassign their controlled entities
        if (info.status == OccupantInfo.DISCONNECTED) {
            reassignControllers(info.bodyOid, false);
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

            takeMemoriesFromRoom(member);
        }

        super.bodyLeft(bodyOid);

        // reassign this occupant's controlled entities
        reassignControllers(bodyOid, false);
    }

    @Override // from PlaceManager
    protected boolean shouldDeclareEmpty (OccupantInfo leaver)
    {
        int hoomans = 0;
        for (OccupantInfo info : _plobj.occupantInfo) {
            if (info instanceof MemberInfo || info instanceof ObserverInfo) {
                hoomans++;
            }
        }
        return (hoomans == 0);
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
     * Slurp memories from incoming avatars and put them into the room.
     */
    protected void addMemoriesToRoom (MemberObject member)
    {
        MemberLocal local = member.getLocal(MemberLocal.class);

        if (local.memories != null) {
            _roomObj.startTransaction();
            try {
                for (EntityMemoryEntry entry : local.memories) {
                    _roomObj.addToMemories(entry);
                }
            } finally {
                _roomObj.commitTransaction();
            }
            local.memories = null;
        }
    }

    /**
     * Take memories from the room and stuff them into outgoing avatars.
     */
    protected void takeMemoriesFromRoom (MemberObject member)
    {
        if (member.avatar == null) {
            member.getLocal(MemberLocal.class).memories = null;
            return;
        }

        // TODO: only do it when the user is guaranteed moving rooms -- Ray
        if (true) {
            List<EntityMemoryEntry> mems = Lists.newArrayList();
            ItemIdent avatar = member.avatar.getIdent();
            for (EntityMemoryEntry entry : _roomObj.memories) {
                if (avatar.equals(entry.item)) {
                    mems.add(entry);
                }
            }
            if (mems.size() > 0) {
                _roomObj.startTransaction();
                try {
                    for (EntityMemoryEntry entry : mems) {
                        _roomObj.removeFromMemories(entry.getKey());
                    }
                } finally {
                    _roomObj.commitTransaction();
                }

                member.getLocal(MemberLocal.class).memories = mems;
            }
        }
    }

    // if the given member is playing an AVRG, make sure it's controlled; if not, control it
    protected void ensureAVRGameControl (MemberObject member)
    {
        if (member.game != null && member.game.avrGame) {
            Controllable reference = new ControllableAVRGame(member.game.gameId);
            EntityControl ctrl = _roomObj.controllers.get(reference);
            if (ctrl == null) {
                log.info("Assigning control [avrGameId=" + member.game.gameId +
                         ", to=" + member.who() + "].");
                _roomObj.addToControllers(new EntityControl(reference, member.getOid()));
            }
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
     */
    protected void doRoomUpdate (final SceneUpdate update, final MemberObject user)
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
                ((MsoySceneRegistry)_screg).memberUpdatedRoom(user, (MsoyScene)_scene);
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
                    Item.DECOR, Item.USED_AS_BACKGROUND, user.getMemberId(), _scene.getId(),
                    decor.itemId, up.decor.itemId, new ComplainingListener<Object>(
                        log, "Unable to update decor usage"));
            }

            // same with background audio - mark new one as used, unmark old one
            AudioData audioData = msoyScene.getAudioData();
            if (audioData != null && audioData.itemId != up.audioData.itemId) { // modified?
                _itemMan.updateItemUsage(
                    Item.AUDIO, Item.USED_AS_BACKGROUND, user.getMemberId(), _scene.getId(),
                    audioData.itemId, up.audioData.itemId, new ComplainingListener<Object>(
                        log, "Unable to update audio usage"));
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
                data.itemType, Item.UNUSED, user.getMemberId(), _scene.getId(),
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
                data.itemType, Item.USED_AS_FURNITURE, user.getMemberId(), _scene.getId(),
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
    protected void reassignControllers (int bodyOid, boolean avrgOnly)
    {
        // determine which items were under the control of this user
        List<Controllable> items = Lists.newArrayList();
        for (EntityControl ctrl : _roomObj.controllers) {
            if (ctrl.controllerOid == bodyOid &&
                (!avrgOnly || (ctrl.controlled instanceof ControllableAVRGame))) {
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
                    log.info("Assigning control [item=" + ctrlable + ", to=" + ctrl.bodyOid + "].");
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
                _roomObj.startTransaction();
                try {
                    for (MemoryRecord mrec : _mems) {
                        _roomObj.addToMemories(mrec.toEntry());
                    }
                } finally {
                    _roomObj.commitTransaction();
                }
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
            protected Collection<MemoryRecord> _mems;
        });
    }

    @Override
    protected void insertOccupantInfo (OccupantInfo info, BodyObject body)
    {
        if (info instanceof ActorInfo && _dynamicActors.size() >= ACTOR_RENDERING_LIMIT) {
            ((ActorInfo)info).useStaticMedia();
        }

        super.insertOccupantInfo(info, body);
    }

    protected void removeAndFlushMemories (ItemIdent item)
    {
        // clear out any memories that were loaded for this item
        List<EntityMemoryEntry> toRemove = Lists.newArrayList();
        for (EntityMemoryEntry entry : _roomObj.memories) {
            if (item.equals(entry.item)) {
                toRemove.add(entry);
            }
        }
        if (!toRemove.isEmpty()) {
            _roomObj.startTransaction();
            try {
                for (EntityMemoryEntry entry : toRemove) {
                    _roomObj.removeFromMemories(entry.getRemoveKey());
                }
            } finally {
                _roomObj.commitTransaction();
            }
            // persist any of the old memories that were modified
            flushMemories(_invoker, _memoryRepo, toRemove);
        }
    }

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
                checkDynamic(event.getEntry(), false);
            }
        }

        // from SetListener
        public void entryUpdated (final EntryUpdatedEvent<OccupantInfo> event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                Runnable onSuccess = new Runnable () {
                    public void run () {
                        updateAvatarIdent(event.getOldEntry(), event.getEntry());
                    }
                };

                if (event.getOldEntry() instanceof MemberInfo) {
                    removeAndFlushMemories(((MemberInfo)event.getOldEntry()).getItemIdent());
                    onSuccess.run();
                }
                if (event.getEntry() instanceof MemberInfo) {
                    resolveMemories(Collections.singleton(
                        ((MemberInfo)event.getEntry()).getItemIdent()),
                        onSuccess);
                }

                checkDynamic(event.getOldEntry(), true);
                checkDynamic(event.getEntry(), false);
            }
        }

        // from SetListener
        public void entryRemoved (EntryRemovedEvent<OccupantInfo> event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent(event.getOldEntry(), null);
                checkDynamic(event.getOldEntry(), true);
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

        /**
         * Adds or removes the occupant to the set of dynamic actors if dynamic.
         */
        protected void checkDynamic (OccupantInfo info, boolean leaving)
        {
            if (info instanceof ActorInfo && !((ActorInfo)info).isStatic()) {
                if (leaving) {
                    ActorInfo prev = _dynamicActors.remove(info.getBodyOid());
                    if (prev == null) {
                        log.warning("Leaving dynamic actor not found in map", "room",
                            _roomObj.which(), "actor", info);
                    }
                } else {
                    ActorInfo prev = _dynamicActors.remove(info.getBodyOid());
                    if (prev != null) {
                        log.warning("Entering dynamic actor already in map", "room",
                            _roomObj.which(), "actor", info);
                    }
                    _dynamicActors.put(info.getBodyOid(), (ActorInfo)info);
                }
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

    /** Game ids of properties we are currently loading. */
    protected ArrayIntSet _pendingGameIds = new ArrayIntSet();

    /** If non-null, a list of memberId blocked from the room. */
    protected ArrayIntSet _booted;

    /** Listens to the room object. */
    protected RoomListener _roomListener = new RoomListener();

    /** Mapping to keep track of spawned mobs. */
    protected Map<Tuple<Integer, String>, MobObject> _mobs = Maps.newHashMap();

    /** Mapping to keep track of starting location of added bodies. */
    protected HashIntMap<Location> _startingLocs = new HashIntMap<Location>();

    /** For all MemberInfo's, a mapping of ItemIdent to the member's oid. */
    protected Map<ItemIdent,Integer> _avatarIdents = Maps.newHashMap();

    /** Non-statically rendered avatars and pets in the room. */
    protected HashIntMap<ActorInfo> _dynamicActors = new HashIntMap<ActorInfo>();

    /** After this level of occupancy is reached, actors are made static. */
    protected static final int ACTOR_RENDERING_LIMIT = 20;

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
