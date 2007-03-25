//
// $Id$

package com.threerings.msoy.world.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.server.SpotSceneManager;

import com.threerings.msoy.data.ActorInfo;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.world.client.RoomService;
import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomMarshaller;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.WorldActorInfo;
import com.threerings.msoy.world.data.WorldMemberInfo;
import com.threerings.msoy.world.data.WorldOccupantInfo;

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
     * Forcibly change the state of an actor.
     */
    public void setState (MsoyBodyObject actor, String state)
    {
        // update the state in the body object
        actor.avatarState = state;

        // update the occupant info
        OccupantInfo occInfo = getOccupantInfo(actor.getOid());
        WorldOccupantInfo winfo = (WorldOccupantInfo) occInfo;
        if (ObjectUtil.equals(winfo.getState(), state)) {
            // no change, no event
            return;
        }

        // TODO: consider, instead of updating the whole dang occInfo,
        // of dispatching a custom event that will update the state
        // and serve as the trigger event to usercode...
        if (occInfo instanceof WorldMemberInfo) {
            ((WorldMemberInfo) occInfo).state = state;

        } else if (occInfo instanceof WorldActorInfo) {
            ((WorldActorInfo) occInfo).state = state;

        } else {
            log.warning("Wtf kind of occupant info is this: " + occInfo);
        }
        updateOccupantInfo(occInfo);
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

        // TODO: avatar access controls
//         // if this is an avatar trigger, make sure it's dispatched by the avatar's owner
//         if (item.type == Item.AVATAR && item.itemId != who.getOid()) {
//             log.warning("Rejecting avatar trigger by non-owner [who=" + who.who() +
//                         ", tgt=" + _roomObj.occupantInfo.get(item.itemId) +
//                         " (" + item + ")].");
//             return;
//         }

        // dispatch this as a simple MessageEvent
        _roomObj.postMessage(RoomCodes.SPRITE_MESSAGE, item, name, arg, isAction);
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

        // Create a list of all item ids
        ArrayList<ItemIdent> list = new ArrayList<ItemIdent>();
        for (FurniData furni : ((MsoyScene) _scene).getFurni()) {
            if (furni.itemType == Item.NOT_A_TYPE) {
                continue;
            }
            list.add(new ItemIdent(furni.itemType, furni.itemId));
        }

        MsoyServer.itemMan.getItems(list, new ResultAdapter<ArrayList<Item>>(listener));
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

        // TODO: if an item is removed from the room, remove any memories from the room object and
        // flush any modifications to the database

        ArrayIntSet scenesToId = null;
        for (SceneUpdate update : updates) {
            // TODO: complicated verification of changes, including verifying that the user owns
            // the items they're adding (and that they don't add any props)

            // furniture modification updates require us to mark item usage
            if (update instanceof ModifyFurniUpdate) {
                ModifyFurniUpdate mfu = (ModifyFurniUpdate) update;
                MsoyServer.itemMan.updateItemUsage(user.getMemberId(),
                    _scene.getId(), mfu.furniRemoved, mfu.furniAdded,
                    new ResultListener<Object>() {
                        public void requestCompleted (Object result) {}
                        public void requestFailed (Exception cause) {
                            log.warning("Unable to update item usage " +
                                "[e=" + cause + "].");
                        }
                    });

                // also, we locate all the scene ids named in added portals
                if (mfu.furniAdded != null) {
                    for (FurniData furni : mfu.furniAdded) {
                        if (furni.actionType == FurniData.ACTION_PORTAL) {
                            try {
                                int sceneId = Integer.parseInt(
                                    furni.splitActionData()[0]);
                                if (scenesToId == null) {
                                    scenesToId = new ArrayIntSet();
                                }
                                scenesToId.add(sceneId);
                            } catch (Exception e) {
                                // just accept it
                            }
                        }
                    }
                }
            }
        }

        if (scenesToId != null) {
            updateRoom2(user, updates, scenesToId);
        } else {
            finishUpdateRoom(user, updates);
        }
    }

    // documentation inherited from RoomProvider
    public void updateMemory (ClientObject caller, final MemoryEntry entry)
    {
        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        MemberObject who = (MemberObject) caller;
        if (!checkAssignControl(who, entry.item, "updateMemory")) {
            return;
        }

        // TODO: verify that the caller is in the scene with this item, that the memory does not
        // exdeed legal size, other item specific restrictions

        // mark it as modified and update the room object; we'll save it when we unload the room
        entry.modified = true;
        if (_roomObj.memories.contains(entry)) {
            _roomObj.updateMemories(entry);
        } else {
            _roomObj.addToMemories(entry);
        }
    }

    // from interface RoomProvider
    public void changeLocation (ClientObject caller, ItemIdent item, Location newloc)
    {
        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        MemberObject who = (MemberObject) caller;
        if (!checkAssignControl(who, item, "changeLocation")) {
            return;
        }

        for (OccupantInfo info : _roomObj.occupantInfo) {
            ActorInfo ainfo = (ActorInfo)info;
            if (ainfo.getItemIdent().equals(item)) {
                _roomObj.updateOccupantLocs(new SceneLocation(newloc, ainfo.getBodyOid()));
            }
        }
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

        _roomObj = (RoomObject) _plobj;
        _roomObj.setRoomService((RoomMarshaller)
                                MsoyServer.invmgr.registerDispatcher(new RoomDispatcher(this)));

        _roomObj.addListener(_roomListener);

        // determine which (if any) items in this room have a memories and load them up
        ArrayIntSet furniIds = new ArrayIntSet();
        for (FurniData furni : ((MsoyScene) _scene).getFurni()) {
            if (furni.itemType == Item.FURNITURE) {
                furniIds.add(furni.itemId);
            }
        }
        if (furniIds.size() > 0) {
            resolveMemories(furniIds);
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

        // shut our pets down
        MsoyServer.petMan.shutdownRoomPets(_roomObj);

        // flush any modified memory records to the database
        final ArrayList<MemoryRecord> memrecs = new ArrayList<MemoryRecord>();
        for (MemoryEntry entry : _roomObj.memories) {
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

    @Override // documentation inherited
    protected SceneLocation computeEnteringLocation (BodyObject body, Portal entry)
    {
        if (body instanceof MemberObject) {
            // automatically add the room to their recent list
            MemberObject memberObj = (MemberObject) body;
            memberObj.addToRecentScenes(_scene.getId(), _scene.getName());
        }

        if (entry != null) {
            return super.computeEnteringLocation(body, entry);
        }
        // fallback if there is no portal
        return new SceneLocation(new MsoyLocation(.5, 0, .5, (short) 0), body.getOid());
    }

    /**
     * updateRoom, continued. Look up any scene names for new portals.
     */
    protected void updateRoom2 (final MemberObject user, final SceneUpdate[] updates,
                                ArrayIntSet scenesToId)
    {
        final int[] sceneIds = scenesToId.toIntArray();
        MsoyServer.invoker.postUnit(new RepositoryUnit("IdentifyScenes") {
            public void invokePersist () throws PersistenceException {
                _sceneNames = MsoyServer.sceneRepo.identifyScenes(sceneIds);
            }
            public void handleSuccess () {
                updateRoom3(user, updates, _sceneNames);
            }
            public void handleFailure (Exception e) {
                log.warning("Unable to identify scenes [err=" + e + "]");
                // just finish off
                finishUpdateRoom(user, updates);
            }
            protected HashIntMap<String> _sceneNames;
        });
    }

    /**
     * Assign the names to portals.
     */
    protected void updateRoom3 (MemberObject user, SceneUpdate[] updates,
                                HashIntMap<String> sceneNames)
    {
        for (SceneUpdate update : updates) {
            if (update instanceof ModifyFurniUpdate) {
                ModifyFurniUpdate mfu = (ModifyFurniUpdate) update;
                if (mfu.furniAdded != null) {
                    for (FurniData furni : mfu.furniAdded) {
                        if (furni.actionType == FurniData.ACTION_PORTAL) {
                            try {
                                int sceneId = Integer.parseInt(furni.splitActionData()[0]);
                                String name = sceneNames.get(sceneId);
                                if (name != null) {
                                    furni.actionData = sceneId + ":" + name;
                                }
                            } catch (Exception e) {
                                // whatever
                            }
                        }
                    }
                }
            }
        }

        finishUpdateRoom(user, updates);
    }

    /**
     * Ah, the final step in updating the room.
     */
    protected void finishUpdateRoom (MemberObject user, SceneUpdate[] updates)
    {
        for (SceneUpdate update : updates) {
            recordUpdate(update);
        }
    }

    /**
     * Loads up all specified memories and places them into the room object.
     */
    protected void resolveMemories (final ArrayIntSet furniIds)
    {
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _mems = MsoyServer.memoryRepo.loadMemories(Item.FURNITURE, furniIds);
                    return true;
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to load memories [where=" + where() +
                            ", ids=" + furniIds + "].", pe);
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
     * Checks to see if an item is being controlled by any client. If not, the calling client is
     * assigned as the item's controller and true is returned. If the item is already being
     * controlled by the calling client, true is returned. Otherwise false is returned (indicating
     * that another client currently has control of the item).
     */
    protected boolean checkAssignControl (MemberObject who, ItemIdent item, String from)
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
            if (info instanceof WorldMemberInfo && info.status == OccupantInfo.ACTIVE) {
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

    /** Listens to the room. */
    protected class RoomListener
        implements SetListener
    {
        // from SetListener
        public void entryAdded (EntryAddedEvent event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent(null, event.getEntry());
            }
        }

        // from SetListener
        public void entryUpdated (EntryUpdatedEvent event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent(event.getOldEntry(), event.getEntry());
            }
        }

        // from SetListener
        public void entryRemoved (EntryRemovedEvent event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent(event.getOldEntry(), null);
            }
        }

        /**
         * Maintain a mapping of ItemIdent -> oid for all WorldMemberInfos.
         */
        protected void updateAvatarIdent (Object oldInfo, Object newInfo)
        {
            // we only track WorldMemberInfo, as those are the only things
            // that represent MemberObjects

            WorldMemberInfo info;
            if (oldInfo instanceof WorldMemberInfo) {
                info = (WorldMemberInfo) oldInfo;
                _avatarIdents.remove(info.getItemIdent());
            }

            if (newInfo instanceof WorldMemberInfo) {
                info = (WorldMemberInfo) newInfo;
                _avatarIdents.put(info.getItemIdent(), info.bodyOid);
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

    /** For all WorldMemberInfo's, a mapping of ItemIdent to the member's oid. */
    protected HashMap<ItemIdent,Integer> _avatarIdents = new HashMap<ItemIdent,Integer>();
}
