//
// $Id$

package com.threerings.msoy.world.server;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.text.MessageUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Comparators;
import com.samskivert.util.ComplainingListener;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.Tuple;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
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
import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.whirled.client.SceneMoveAdapter;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.server.SpotSceneManager;

//import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.RoomName;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.BootablePlaceManager;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MsoyEventLogger;

import com.threerings.msoy.chat.server.ChatChannelManager;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemManager;

import com.threerings.msoy.world.client.RoomService;
import com.threerings.msoy.world.data.ActorInfo;
import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.Controllable;
import com.threerings.msoy.world.data.ControllableAVRGame;
import com.threerings.msoy.world.data.ControllableEntity;
import com.threerings.msoy.world.data.EffectData;
import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.FurniUpdate;
import com.threerings.msoy.world.data.MemberInfo;
import com.threerings.msoy.world.data.MobObject;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.RoomPropertyEntry;
import com.threerings.msoy.world.data.SceneAttrsUpdate;
import com.threerings.msoy.world.server.persist.MemoryRecord;
import com.threerings.msoy.world.server.persist.MemoryRepository;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;

import static com.threerings.msoy.Log.log;

/**
 * Manages a "Room".
 */
@EventThread
public class RoomManager extends SpotSceneManager
    implements RoomProvider, BootablePlaceManager
{
    /**
     * Flush any modified memories contained within the specified Iterable.
     */
    public static void flushMemories (Invoker invoker, final MemoryRepository memoryRepo,
                                      Iterable<EntityMemoryEntry> entries)
    {
        final List<MemoryRecord> memrecs = Lists.newArrayList();
        for (EntityMemoryEntry entry : entries) {
            if (entry.modified) {
                memrecs.add(new MemoryRecord(entry));
            }
        }
        if (memrecs.size() > 0) {
            invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    try {
                        memoryRepo.storeMemories(memrecs);
                    } catch (PersistenceException pe) {
                        log.warning("Failed to update memories " + memrecs + ".", pe);
                    }
                    return false;
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
     * Add a transient effect on the specified body.
     */
    public void addTransientEffect (int bodyOid, MediaDesc media)
    {
        addTransientEffect(bodyOid, media, EffectData.MODE_NONE, null);
    }

    /**
     * Add a transient effect on the specified body.
     */
    public void addTransientEffect (int bodyOid, MediaDesc media, byte paramMode, String parameter)
    {
        EffectData effect = new EffectData();
        effect.media = media;
        effect.setParameter(paramMode, parameter);
        _roomObj.postMessage(RoomObject.ADD_EFFECT, bodyOid, effect);
    }

    /**
     * Create and add an effect.
     */
    public EffectData addEffect (MediaDesc media, MsoyLocation loc, byte roomLayer)
    {
        EffectData effect = createEffect(media, loc, roomLayer);
        _roomObj.addToEffects(effect);
        return effect;
    }

    /**
     * Create a new effect for use in this room.
     */
    public EffectData createEffect (MediaDesc media, MsoyLocation loc, byte roomLayer)
    {
        EffectData effect = new EffectData();
        effect.id = _nextEffectId++;
        effect.media = media;
        effect.loc = loc;
        effect.roomLayer = roomLayer;
        return effect;
    }

    /**
     * Create a new effect for use in this room.
     */
    public EffectData createEffect (MediaDesc media, MsoyLocation loc)
    {
        return createEffect(media, loc, RoomCodes.FURNITURE_LAYER);
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

        // and from the chat channel
// TODO: this doesn't work. It ends up removing the user from the list of chatters, but they
// are still subscribed to the channel. Fuck if I know.
//        _channelMan.leaveChannel(bootee,
//            ChatChannel.makeRoomChannel(new RoomName(_scene.getName(), _scene.getId())));

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

    // documentation inherited from RoomProvider
    public void requestControl (ClientObject caller, ItemIdent item)
    {
        ensureEntityControl((MemberObject) caller, item, "requestControl");
        // TODO: throw invocationexception on failure?
    }

    // documentation inherited from RoomProvider
    public void sendSpriteMessage (ClientObject caller, ItemIdent item, String name, byte[] arg,
                                   boolean isAction)
    {
        // make sure the caller is in the room
        MemberObject who = (MemberObject)caller;
        if (!_roomObj.occupants.contains(who.getOid())) {
            log.warning("Rejecting sprite message request by non-occupant [who=" + who.who() +
                        ", item=" + item + ", name=" + name + "].");
            return;
        }

        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        if (isAction && !ensureEntityControl(who, item, "triggerAction")) {
            log.info("Dropping sprite message for lack of control [who=" + who.who() +
                     ", item=" + item + ", name=" + name + "].");
            return;
        }

        // dispatch this as a simple MessageEvent
        _roomObj.postMessage(RoomCodes.SPRITE_MESSAGE, item, name, arg, isAction);
    }

    // documentation inherited from RoomProvider
    public void sendSpriteSignal (ClientObject caller, String name, byte[] arg)
    {
        // make sure the caller is in the room
        MemberObject who = (MemberObject)caller;
        if (!_roomObj.occupants.contains(who.getOid())) {
            log.warning("Rejecting sprite signal request by non-occupant [who=" + who.who() +
                        ", name=" + name + "].");
            return;
        }

        // dispatch this as a simple MessageEvent
        _roomObj.postMessage(RoomCodes.SPRITE_SIGNAL, name, arg);
    }

    // documentation inherited from RoomProvider
    public void setActorState (ClientObject caller, ItemIdent item, int actorOid, String state)
    {
        MemberObject who = (MemberObject) caller;
        if (!_roomObj.occupants.contains(who.getOid())) {
            log.warning("Rejecting actor state request by non-occupant [who=" + who.who() +
                        ", item=" + item + ", state=" + state + "].");
            return;
        }

        // make sure the actor to be state-changed is also in this room
        MsoyBodyObject actor;
        if (who.getOid() != actorOid) {
            if (!_roomObj.occupants.contains(actorOid)) {
                log.warning("Rejecting actor state request for non-occupant [who=" + who.who() +
                    ", item=" + item + ", state=" + state + "].");
                return;
            }
            actor = (MsoyBodyObject) _omgr.getObject(actorOid);

        } else {
            // the actor is the caller
            actor = who;
        }

        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        if (!ensureEntityControl(who, item, "setState")) {
            log.info("Dropping change state for lack of control [who=" + who.who() +
                     ", item=" + item + ", state=" + state + "].");
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
            public void invokePersist () throws PersistenceException {
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

    // documentation inherited from RoomProvider
    public void updateMemory (ClientObject caller, final EntityMemoryEntry entry)
    {
// NOTE: I've disabled the need to be in control to update memory (Ray July 6, 2007)
//        // if this client does not currently control this entity; ignore the request; if no one
//        // controls it, this will assign this client as controller
//        MemberObject who = (MemberObject) caller;
//        if (!checkAssignControl(who, entry.item, "updateMemory")) {
//            return;
//        }

        // TODO: verify that the caller is in the scene with this item, other item specific
        // restrictions

        // verify that the memory does not exdeed legal size
        int totalSize = 0;
        for (EntityMemoryEntry rent : _roomObj.memories) {
            if (rent.item.equals(entry.item) && !rent.key.equals(entry.key)) {
                totalSize += rent.getSize();
            }
        }
        if (totalSize + entry.getSize() > MAX_MEMORY_SIZE) {
            log.info("Rejecting memory update as too large [otherSize=" + totalSize +
                     ", newEntrySize=" + entry.getSize() + "].");
            return; // no feedback, just don't update it
        }

        // mark it as modified and update the room object; we'll save it when we unload the room
        entry.modified = true;
        if (_roomObj.memories.contains(entry)) {
            _roomObj.updateMemories(entry);
        } else {
            _roomObj.addToMemories(entry);
        }
    }

    // documentation inherited from RoomProvider
    public void setRoomProperty (ClientObject caller, final RoomPropertyEntry entry)
    {
        // Make sure this property won't put us over the total size limit
        int totalSize = 0;
        for (RoomPropertyEntry rent : _roomObj.roomProperties) {
            if ( ! rent.key.equals(entry.key)) {
                totalSize += rent.getSize();
            }
        }
        if (totalSize + entry.getSize() > MAX_ROOM_PROPERTY_SIZE) {
            log.info("Rejecting room property update as too large [otherSize=" + totalSize +
                     ", newEntrySize=" + entry.getSize() + "].");
            return; // no feedback, just don't update it
        }

        if (_roomObj.roomProperties.contains(entry)) {
            _roomObj.updateRoomProperties(entry);
        } else {
            _roomObj.addToRoomProperties(entry);
        }
    }

    // from interface RoomProvider
    public void changeLocation (ClientObject caller, ItemIdent item, Location newLoc)
    {
        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        MemberObject who = (MemberObject) caller;
        if (!ensureEntityControl(who, item, "changeLocation")) {
            return;
        }

        int oid = findActorOid(item);
        if (oid != 0) {
            _roomObj.updateOccupantLocs(new SceneLocation(newLoc, oid));
        }
    }

    // from RoomProvider
    public void spawnMob (ClientObject caller, int gameId, String mobId, String mobName,
                          final InvocationListener listener)
        throws InvocationException
    {
        Tuple<Integer, String> key = new Tuple<Integer, String>(gameId, mobId);
        if (_mobs.containsKey(key)) {
            log.warning("Tried to spawn mob that's already present [gameId=" +
                        gameId + ", mobId=" + mobId + "]");
            listener.requestFailed(RoomCodes.E_INTERNAL_ERROR);
            return;
        }

        if (mobName == null || mobName.length() == 0) {
            throw new IllegalArgumentException(
                "Mob spawn request without name [gameId=" + gameId + ", mobId=" + mobId + "]");
        }

        final MobObject mobObj = _omgr.registerObject(new MobObject());
        mobObj.setGameId(gameId);
        mobObj.setIdent(mobId);
        mobObj.setUsername(new Name(mobName));
        _mobs.put(key, mobObj);

        // then enter the scene like a proper scene entity
        _screg.moveTo(mobObj, getScene().getId(), -1, new SceneMoveAdapter() {
            public void requestFailed (String reason) {
                log.warning("MOB failed to enter scene [mob=" + mobObj + ", scene=" +
                           getScene().getId() + ", reason=" + reason + "].");
                listener.requestFailed(reason);
            }
        });
    }

    // from RoomProvider
    public void despawnMob (ClientObject caller, int gameId, String mobId,
                            final InvocationListener listener)
        throws InvocationException
    {
        Tuple<Integer, String> key = new Tuple<Integer, String>(gameId, mobId);

        final MobObject mobObj = _mobs.get(key);
        if (mobObj == null) {
            log.warning("Tried to despawn mob that's not present [gameId=" +
                        gameId + ", mobId=" + mobId + "]");
            listener.requestFailed(RoomCodes.E_INTERNAL_ERROR);
            return;
        }

        _screg.leaveOccupiedScene(mobObj);
        _omgr.destroyObject(mobObj.getOid());
        _mobs.remove(key);
    }

    @Override // from PlaceManager
    public void messageReceived (MessageEvent event)
    {
        // we want to explicitly disable the standard method calling by name that we allow in more
        // trusted environments
    }

    public void occupantLeftAVRGame (MemberObject member)
    {
        reassignControllers(member.getOid(), true);
    }

    public void occupantEnteredAVRGame (MemberObject member)
    {
        ensureAVRGameControl(member);
    }

    /**
     * Checks to see if an item is being controlled by any client. If not, the calling client is
     * assigned as the item's controller and true is returned. If the item is already being
     * controlled by the calling client, true is returned. Otherwise false is returned (indicating
     * that another client currently has control of the item).
     */
    public boolean ensureEntityControl (MemberObject who, ItemIdent item, String from)
    {
        Integer memberOid = _avatarIdents.get(item);
        if (memberOid != null) {
            if (who.getOid() == memberOid.intValue()) {
                // yes, you may control your own avatar
                return true;
            }
            log.warning("Some user is trying to control another's avatar! " +
                "[who=" + who.who() + ", avatar=" + item + "].");
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

        // determine which (if any) items in this room have memories and load them up
        List<ItemIdent> memoryIds = Lists.newArrayList();
        for (FurniData furni : ((MsoyScene) _scene).getFurni()) {
            if (furni.itemType != Item.NOT_A_TYPE) {
                memoryIds.add(furni.getItemIdent());
            }
        }
        if (memoryIds.size() > 0) {
            resolveMemories(memoryIds);
        }

        // load up any pets that are "let out" in this room scene
        _petMan.loadRoomPets(_roomObj, _scene.getId());
    }

    @Override // from PlaceManager
    protected void bodyEntered (final int bodyOid)
    {
        super.bodyEntered(bodyOid);

        DObject body = _omgr.getObject(bodyOid);
        if (body instanceof MemberObject) {
            MemberObject member = (MemberObject) body;
            ensureAVRGameControl(member);
            MsoySceneModel model = (MsoySceneModel) getScene().getSceneModel();
            boolean isMemberScene = (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER);
            member.metrics.room.init(isMemberScene, model.ownerId);

            if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                member.getStats().addToSetStat(StatType.WHIRLEDS_VISITED, model.ownerId);
            }
        }
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
            MemberObject member = (MemberObject) body;
            member.metrics.room.save(member);

            // get the last known occupancy length - this might have been measured above,
            // or by the peer serialization code if we're moving across servers
            int secondsInRoom = member.metrics.room.getLastOccupancyLength();
            MsoySceneModel model = (MsoySceneModel) getScene().getSceneModel();
            boolean isWhirled = (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP);

            // if this is not a transient viewer, log it!
            if (! member.memberName.isViewer()) {
                _eventLog.roomLeft(
                    member.getMemberId(), model.sceneId, isWhirled,
                    secondsInRoom, _roomObj.occupants.size(), member.referral.tracker);
            }
        }

        super.bodyLeft(bodyOid);

        // reassign this occupant's controlled entities
        reassignControllers(bodyOid, false);
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

    @Override // documentation inherited
    protected SceneLocation computeEnteringLocation (BodyObject body, Portal from, Portal entry)
    {
//        if (body instanceof MemberObject) {
//            // automatically add the room to their recent list
//            MemberObject memberObj = (MemberObject) body;
//            memberObj.addToRecentScenes(_scene.getId(), _scene.getName());
//        }

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
    protected void doRoomUpdate (SceneUpdate update, MemberObject user)
    {
        // TODO: complicated verification of changes, including verifying that the user owns any
        // item they're adding, etc.

        if (update instanceof SceneAttrsUpdate) {
            SceneAttrsUpdate up = (SceneAttrsUpdate) update;
            MsoyScene msoyScene = (MsoyScene) _scene;

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

            // if the name was modified, we need to notify the chat channel manager so it can
            // update the channel name.
            if (nameChange) {
                _channelMan.updateRoomChannelName(
                    new RoomName(up.name, msoyScene.getId()));
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
            List<EntityMemoryEntry> toRemove = Lists.newArrayList();
            if (data.itemType != Item.NOT_A_TYPE) {
                ItemIdent ident = data.getItemIdent();
                for (EntityMemoryEntry entry : _roomObj.memories) {
                    if (ident.equals(entry.item)) {
                        toRemove.add(entry);
                    }
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

        } else if (update instanceof FurniUpdate.Add) {
            // mark this item as in use
            FurniData data = ((FurniUpdate)update).data;
            _itemMan.updateItemUsage(
                data.itemType, Item.USED_AS_FURNITURE, user.getMemberId(), _scene.getId(),
                0, data.itemId, new ComplainingListener<Object>(
                    log, "Unable to set furni item usage"));

            // and resolve any memories it may have
            resolveMemories(Collections.singleton(data.getItemIdent()));
        }

        // initialize and record this update to the scene management system (which will persist it,
        // send it to the client for application to the scene, etc.)
        update.init(_scene.getId(), _scene.getVersion());
        recordUpdate(update);

        // let the registry know that rooms be gettin' updated (TODO: don't do this on every
        // fucking update, it's super expensive)
        ((MsoySceneRegistry)_screg).memberUpdatedRoom(user, (MsoyScene)_scene);
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
    protected void resolveMemories (final Collection<ItemIdent> idents)
    {
        _invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _mems = _memoryRepo.loadMemories(idents);
                    return !_mems.isEmpty();
                } catch (PersistenceException pe) {
                    log.warning("Failed to load memories [where=" + where() +
                            ", ids=" + idents + "].", pe);
                    return false;
                }
            };

            public void handleResult () {
                _roomObj.startTransaction();
                try {
                    for (MemoryRecord mrec : _mems) {
                        _roomObj.addToMemories(mrec.toEntry());
                    }
                } finally {
                    _roomObj.commitTransaction();
                }
            }

            protected Collection<MemoryRecord> _mems;
        });
    }

    /** Listens to the room. */
    protected class RoomListener
        implements SetListener
    {
        // from SetListener
        public void entryAdded (EntryAddedEvent event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent(null, (OccupantInfo)event.getEntry());
            }
        }

        // from SetListener
        public void entryUpdated (EntryUpdatedEvent event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent((OccupantInfo)event.getOldEntry(), (OccupantInfo)event.getEntry());
            }
        }

        // from SetListener
        public void entryRemoved (EntryRemovedEvent event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent((OccupantInfo)event.getOldEntry(), null);
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

    /** If non-null, a list of memberId blocked from the room. */
    protected ArrayIntSet _booted;

    /** Listens to the room object. */
    protected RoomListener _roomListener = new RoomListener();

    /** Mapping to keep track of spawned mobs. */
    protected Map<Tuple<Integer, String>, MobObject> _mobs = Maps.newHashMap();

    /** For all MemberInfo's, a mapping of ItemIdent to the member's oid. */
    protected Map<ItemIdent,Integer> _avatarIdents = Maps.newHashMap();

    /** The next id to use for an effect. */
    protected short _nextEffectId;

    /** The maximum size of an entity's memory, including all keys and values. */
    protected static final int MAX_MEMORY_SIZE = 4096;

    /** The maximum size of a room's properties, including all keys and values. */
    protected static final int MAX_ROOM_PROPERTY_SIZE = 4096;

    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected ChatChannelManager _channelMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected PetManager _petMan;
    @Inject protected SceneRegistry _screg;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected MemberLocator _locator;
    @Inject protected MsoyEventLogger _eventLog;
}
