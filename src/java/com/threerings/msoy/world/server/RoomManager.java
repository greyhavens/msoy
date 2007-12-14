//
// $Id$

package com.threerings.msoy.world.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;

import com.google.common.collect.Maps;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntTuple;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;
import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.server.InvocationException;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.util.Name;
import com.threerings.whirled.client.SceneMoveAdapter;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.server.SpotSceneManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.person.util.FeedMessageType;

import com.threerings.msoy.world.client.RoomService;
import com.threerings.msoy.world.data.ActorInfo;
import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.EffectData;
import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MemberInfo;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.MobObject;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.PetObject;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomMarshaller;
import com.threerings.msoy.world.data.RoomPropertyEntry;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.SceneAttrsUpdate;

import com.threerings.msoy.world.server.persist.MemoryRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages a "Room".
 */
public class RoomManager extends SpotSceneManager
    implements RoomProvider
{
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
            // return the error string
            return ie.getMessage();
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

    @Override // from SpotSceneManager
    public void willTraversePortal (BodyObject body, Portal portal)
    {
        MsoyLocation loc = (MsoyLocation) portal.getLocation();
        // We need to set the body's orientation to match the approach to the portal.
        // Look up their current location and move them from there. This could be a little
        // "off" if their sprite has not yet walked to this location, but oh well.
        SceneLocation sloc = (SceneLocation) _roomObj.occupantLocs.get(body.getOid());
        if (sloc != null) {
            MsoyLocation origin = (MsoyLocation) sloc.loc;
            double radians = Math.atan2(loc.z - origin.z, loc.x - origin.x);
            // turn the radians into a positive degree value in the whirled orientation space
            loc.orient = (short) ((360 + 90 + (int) Math.round(Math.toDegrees(radians))) % 360);
        }

        updateLocation(body, loc);
    }

    // documentation inherited from RoomProvider
    public void requestControl (ClientObject caller, ItemIdent item)
    {
        MemberObject member = (MemberObject) caller;
        boolean gotControl = checkAssignControl(member, item, "requestControl");
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
        if (isAction && !checkAssignControl(who, item, "triggerAction")) {
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
            actor = (MsoyBodyObject) MsoyServer.omgr.getObject(actorOid);

        } else {
            // the actor is the caller
            actor = who;
        }

        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        if (!checkAssignControl(who, item, "setState")) {
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
        if (!((MsoyScene) _scene).canEdit((MemberObject) caller)) {
            throw new InvocationException(RoomCodes.E_ACCESS_DENIED);
        }

        // for now send back a TRUE
        listener.requestProcessed(Boolean.TRUE);
    }

    // documentation inherited from RoomProvider
    public void updateRoom (ClientObject caller, SceneUpdate[] updates,
                            RoomService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        if (!((MsoyScene) _scene).canEdit(user)) {
            throw new InvocationException(RoomCodes.E_ACCESS_DENIED);
        }
        doRoomUpdate(updates, user);
    }

    // from interface RoomProvider
    public void purchaseRoom (ClientObject caller, final RoomService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;

        // make sure they have editing privileges in this scene as they will be adding to this
        // scene group
        MsoyScene scene = (MsoyScene) _scene;
        if (!scene.canEdit(user)) {
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

        MsoyServer.invoker.postUnit(new RepositoryUnit("purchaseRoom") {
            public void invokePersist () throws PersistenceException {
                _newRoomId = MsoyServer.sceneRepo.createBlankRoom(
                    ownerType, ownerId, roomName, portalAction, false);
            }
            public void handleSuccess () {
                user.addToOwnedScenes(new SceneBookmarkEntry(_newRoomId, roomName, 0));
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
     * Validates the member against the scene's access control flag.
     * If not successful, throws the invocation exception, with failure reason in the message.
     */
    public void validateEntranceAction (BodyObject body)
        throws InvocationException
    {
        if (!((MsoyScene) _scene).canEnter((MemberObject)body)) {
            throw new InvocationException(RoomCodes.E_ENTRANCE_DENIED);
        }
    }

    /**
     * reclaim an item from this room.
     */
    public void reclaimItem (ItemIdent item, MemberObject user)
    {
        for (FurniData furni : ((MsoyScene)_scene).getFurni()) {
            if (item.equals(furni.getItemIdent())) {
                ModifyFurniUpdate update = new ModifyFurniUpdate();
                update.initialize(_scene.getId(), _scene.getVersion(), new FurniData[] { furni },
                    null);
                doRoomUpdate(new SceneUpdate[] { update }, user);
                break;
            }
        }
    }

    /**
     * Reclaim this room's decor.
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
        doRoomUpdate(new SceneUpdate[] { update }, user);
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
        doRoomUpdate(new SceneUpdate[] { update }, user);
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
        if (entry.key.length() > RoomPropertyEntry.MAX_KEY_LENGTH ||
           (entry.value != null && entry.value.length > RoomPropertyEntry.MAX_VALUE_LENGTH)) {
            log.info("Rejecting memory update, key or value are too long [entry=" + entry + "]");

        } else if (_roomObj.roomProperties.contains(entry)) {
            if (_roomObj.roomProperties.size() >= RoomPropertyEntry.MAX_ENTRIES) {
                log.info("Rejecting memory update, store is full [entry=" + entry + "]");
            } else {
                _roomObj.updateRoomProperties(entry);
            }

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
        if (!checkAssignControl(who, item, "changeLocation")) {
            return;
        }

        int oid = findActorOid(item);
        if (oid != 0) {
            _roomObj.updateOccupantLocs(new SceneLocation(newLoc, oid));
        }
    }

    // from RoomProvider
    public void spawnMob (ClientObject caller, int gameId, String mobId,
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

        final MobObject mobObj = MsoyServer.omgr.registerObject(new MobObject());
        mobObj.setGameId(gameId);
        mobObj.setIdent(mobId);
        mobObj.setUsername(new Name(""));
        _mobs.put(key, mobObj);

        // then enter the scene like a proper scene entity
        MsoyServer.screg.moveTo(mobObj, getScene().getId(), -1, new SceneMoveAdapter() {
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

        MsoyServer.screg.leaveOccupiedScene(mobObj);
        MsoyServer.omgr.destroyObject(mobObj.getOid());
        _mobs.remove(key);
    }

    @Override // from PlaceManager
    public void messageReceived (MessageEvent event)
    {
        // we want to explicitly disable the standard method calling by name that we allow in more
        // trusted environments
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
        _roomObj.setRoomService((RoomMarshaller)
                                MsoyServer.invmgr.registerDispatcher(new RoomDispatcher(this)));
        _roomObj.addListener(_roomListener);

        // register ourselves in our peer object
        MsoyServer.peerMan.roomDidStartup(_scene.getId(), _scene.getName(),
                                          ((MsoyScene) _scene).getAccessControl());

        // determine which (if any) items in this room have memories and load them up
        ArrayList<ItemIdent> memoryIds = new ArrayList<ItemIdent>();
        for (FurniData furni : ((MsoyScene) _scene).getFurni()) {
            if (furni.itemType != Item.NOT_A_TYPE) {
                memoryIds.add(furni.getItemIdent());
            }
        }
        if (memoryIds.size() > 0) {
            resolveMemories(memoryIds);
        }

        // load up any pets that are "let out" in this room scene
        MsoyServer.petMan.loadRoomPets(_roomObj, _scene.getId());
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
        super.bodyLeft(bodyOid);

        // reassign this occupant's controlled entities
        reassignControllers(bodyOid);
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        _roomObj.removeListener(_roomListener);
        MsoyServer.invmgr.clearDispatcher(_roomObj.roomService);

        super.didShutdown();

        // clear out our peer hosting information
        MsoyServer.peerMan.roomDidShutdown(_scene.getId());

        // shut our pets down
        MsoyServer.petMan.shutdownRoomPets(_roomObj);

        // flush any modified memory records to the database
        flushMemories(_roomObj.memories);
    }

    @Override // documentation inherited
    protected SceneLocation computeEnteringLocation (BodyObject body, Portal from, Portal entry)
    {
        if (body instanceof MemberObject) {
            // automatically add the room to their recent list
            MemberObject memberObj = (MemberObject) body;
            memberObj.addToRecentScenes(_scene.getId(), _scene.getName());
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
    protected void doRoomUpdate (SceneUpdate[] updates, MemberObject user)
    {
        // track memory comings and goings
        ArrayList<ItemIdent> oldIdents = new ArrayList<ItemIdent>();
        ArrayList<ItemIdent> newIdents = new ArrayList<ItemIdent>();

        for (SceneUpdate update : updates) {
            // TODO: complicated verification of changes, including verifying that the user owns
            // the items they're adding (and that they don't add any props)

            if (update instanceof SceneAttrsUpdate) {
                SceneAttrsUpdate up = (SceneAttrsUpdate) update;
                MsoyScene msoyScene = (MsoyScene) _scene;
                ResultListener<Object> defaultListener =
                    new ResultListener<Object>() {
                    public void requestCompleted (Object result) {}
                    public void requestFailed (Exception cause) {
                        log.warning("Unable to update decor usage [e=" + cause + "].");
                    }
                };

                // if decor was modified, we should mark new decor as used, and clear the old one
                Decor decor = msoyScene.getDecor();
                if (decor != null && decor.itemId != up.decor.itemId) { // modified?
                    MsoyServer.itemMan.updateItemUsage(
                        Item.DECOR, Item.USED_AS_BACKGROUND, user.getMemberId(), _scene.getId(),
                        decor.itemId, up.decor.itemId, defaultListener);
                }

                // same with background audio - mark new one as used, unmark old one
                AudioData audioData = msoyScene.getAudioData();
                if (audioData != null && audioData.itemId != up.audioData.itemId) { // modified?
                    MsoyServer.itemMan.updateItemUsage(
                        Item.AUDIO, Item.USED_AS_BACKGROUND, user.getMemberId(), _scene.getId(),
                        audioData.itemId, up.audioData.itemId, defaultListener);
                }

                // if the name or access controls were modified, we need to update our HostedPlace
                if (msoyScene.getAccessControl() != up.accessControl ||
                    !msoyScene.getName().equals(up.name)) {
                    MsoyServer.peerMan.roomUpdated(_scene.getId(), up.name,
                                                  up.accessControl);
                }
            }

            // furniture modification updates require us to mark item usage
            if (update instanceof ModifyFurniUpdate) {
                ModifyFurniUpdate mfu = (ModifyFurniUpdate) update;
                MsoyServer.itemMan.updateItemUsage(
                    user.getMemberId(), _scene.getId(), mfu.furniRemoved, mfu.furniAdded,
                    new ResultListener<Object>() {
                    public void requestCompleted (Object result) {}
                    public void requestFailed (Exception cause) {
                        log.warning("Unable to update item usage [e=" + cause + "].");
                    }
                });

                // note memory comings and goings
                if (mfu.furniRemoved != null) {
                    for (FurniData furni : mfu.furniRemoved) {
                        if (furni.itemType != Item.NOT_A_TYPE) {
                            oldIdents.add(furni.getItemIdent());
                        }
                    }
                }
                if (mfu.furniAdded != null) {
                    for (FurniData furni : mfu.furniAdded) {
                        if (furni.itemType != Item.NOT_A_TYPE) {
                            newIdents.add(furni.getItemIdent());
                        }
                    }
                }
            }
        }

        // record our updates
        for (SceneUpdate update : updates) {
            recordUpdate(update);
        }

        // now fix up the lists: any ItemIdent that appears in both lists should be in neither.
        ArrayList<EntityMemoryEntry> oldMemories = new ArrayList<EntityMemoryEntry>();
        for (Iterator<ItemIdent> itr = oldIdents.iterator(); itr.hasNext(); ) {
            ItemIdent ident = itr.next();
            if (newIdents.remove(ident)) {
                itr.remove();
            } else {
                // ah, this is an item that's actually being removed. Locate any memories.
                for (EntityMemoryEntry entry : _roomObj.memories) {
                    if (ident.equals(entry.item)) {
                        oldMemories.add(entry);
                    }
                }
            }
        }

        // now remove any old memories from the room object and persist them
        if (!oldMemories.isEmpty()) {
            _roomObj.startTransaction();
            try {
                for (EntityMemoryEntry entry : oldMemories) {
                    _roomObj.removeFromMemories(entry.getRemoveKey());
                }
            } finally {
                _roomObj.commitTransaction();
            }

            // persist any of the old memories that were modified
            flushMemories(oldMemories);
        }

        // finally, load any new memories
        if (!newIdents.isEmpty()) {
            resolveMemories(newIdents);
        }

        // mark this room change in the member's feed
        publishMemberUpdate(user.getMemberId());
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
     * Checks to see if an item is being controlled by any client. If not, the calling client is
     * assigned as the item's controller and true is returned. If the item is already being
     * controlled by the calling client, true is returned. Otherwise false is returned (indicating
     * that another client currently has control of the item).
     */
    public boolean checkAssignControl (MemberObject who, ItemIdent item, String from)
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

        EntityControl ctrl = _roomObj.controllers.get(item);
        if (ctrl == null) {
            log.info("Assigning control [item=" + item + ", to=" + who.who() + "].");
            _roomObj.addToControllers(new EntityControl(item, who.getOid()));
            return true;
        }
        return (ctrl.controllerOid == who.getOid());
    }

    /**
     * Reassigns all scene entities controlled by the specified client to new controllers.
     */
    protected void reassignControllers (int bodyOid)
    {
        // determine which items were under the control of this user
        ArrayList<ItemIdent> items = new ArrayList<ItemIdent>();
        for (EntityControl ctrl : _roomObj.controllers) {
            if (ctrl.controllerOid == bodyOid) {
                items.add(ctrl.ident);
            }
        }
        if (items.size() == 0) {
            return;
        }

        // clear out the old controller mappings
        try {
            _roomObj.startTransaction();
            for (ItemIdent item : items) {
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
    protected boolean assignControllers (Collection<ItemIdent> items)
    {
        // determine the available controllers
        HashIntMap<Controller> controllers = new HashIntMap<Controller>();
        for (OccupantInfo info : _roomObj.occupantInfo) {
            if (info instanceof MemberInfo && info.status == OccupantInfo.ACTIVE) {
                controllers.put(info.bodyOid, new Controller(info.bodyOid));
            }
        }

        // if we have no potential controllers, the items will remain uncontrolled (which is much
        // better than them being out of control :)
        if (controllers.size() == 0) {
            return false;
        }

        // note the current load of these controllers
        for (EntityControl ctrl : _roomObj.controllers) {
            Controller owner = controllers.get(ctrl.controllerOid);
            if (owner != null) {
                owner.items++;
            }
        }

        // choose the least loaded controller, remove them from the set, assign them control of an
        // item, add them back to the set, then move to the next item
        try {
            _roomObj.startTransaction();
            TreeSet<Controller> set = new TreeSet<Controller>(controllers.values());
            for (ItemIdent item : items) {
                Controller ctrl = set.first();
                set.remove(ctrl);
                ctrl.items++;
                log.info("Assigning control [item=" + item + ", to=" + ctrl.bodyOid + "].");
                _roomObj.addToControllers(new EntityControl(item, ctrl.bodyOid));
                set.add(ctrl);
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
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _mems = MsoyServer.memoryRepo.loadMemories(idents);
                    return true;
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to load memories [where=" + where() +
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

    /**
     * Flush any modified memories contained within the specified Iterable.
     */
    protected void flushMemories (Iterable<EntityMemoryEntry> entries)
    {
        final ArrayList<MemoryRecord> memrecs = new ArrayList<MemoryRecord>();
        for (EntityMemoryEntry entry : entries) {
            if (entry.modified) {
                memrecs.add(new MemoryRecord(entry));
            }
        }
        if (memrecs.size() > 0) {
            MsoyServer.invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    try {
                        MsoyServer.memoryRepo.storeMemories(memrecs);
                    } catch (PersistenceException pe) {
                        log.log(Level.WARNING, "Failed to update memories [where=" + where() +
                                ", memrecs=" + memrecs + "].", pe);
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Publishes to the room owner's feed that they've updated the room.
     */
    protected void publishMemberUpdate (final int memberId)
    {
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    MsoyServer.feedRepo.publishMemberMessage(memberId,
                            FeedMessageType.FRIEND_UPDATED_ROOM,
                            String.valueOf(_scene.getId()) + "\t" + _scene.getName());
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to publish feed [where=" + where() +
                            ", memberId=" + memberId + "].", pe);
                }
                return false;
            }
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
        public int bodyOid;
        public int items;

        public Controller (int bodyOid) {
            this.bodyOid = bodyOid;
        }

        public boolean equals (Object other) {
            return ((Controller)other).bodyOid == bodyOid;
        }
        public int hashCode () {
            return bodyOid;
        }
        public int compareTo (Controller other) {
            return (items - other.items);
        }
    } // End: static class Controller

    /** The room object. */
    protected RoomObject _roomObj;

    /** Listens to the room object. */
    protected RoomListener _roomListener = new RoomListener();

    /** Mapping to keep track of spawned mobs. */
    protected Map<Tuple<Integer, String>, MobObject> _mobs =
        new HashMap<Tuple<Integer,String>, MobObject>();

    /** For all MemberInfo's, a mapping of ItemIdent to the member's oid. */
    protected Map<ItemIdent,Integer> _avatarIdents = Maps.newHashMap();

    /** The next id to use for an effect. */
    protected short _nextEffectId;

    /** The maximum size of an entity's memory, including all keys and values. */
    protected static final int MAX_MEMORY_SIZE = 4096;
}
