//
// $Id$

package com.threerings.msoy.world.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.server.SpotSceneManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomMarshaller;
import com.threerings.msoy.world.data.RoomObject;

import com.threerings.msoy.world.server.persist.MemoryRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages a "Room".
 */
public class RoomManager extends SpotSceneManager
    implements RoomProvider
{
    // documentation inherited from RoomProvider
    public void triggerEvent (ClientObject caller, ItemIdent item, String event)
    {
        // make sure the caller is in the room
        MemberObject who = (MemberObject)caller;
        if (_roomObj.occupants.contains(who.getOid())) {
            log.warning("Rejecting event trigger request by non-occupant [who=" + who.who() +
                        ", item=" + item + ", event=" + event + "].");
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
        _roomObj.postMessage(RoomCodes.TRIGGER_EVENT, item, event);
    }

    // documentation inherited from RoomProvider
    public void editRoom (ClientObject caller, InvocationService.ResultListener listener)
        throws InvocationException
    {
        if (!((MsoyScene) _scene).canEdit((MemberObject) caller)) {
            throw new InvocationException(ACCESS_DENIED);
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
                            InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        if (!((MsoyScene) _scene).canEdit(user)) {
            throw new InvocationException(ACCESS_DENIED);
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
        // TODO: verify that the caller is in the scene with this item, that the memory does not
        // exdeed legal size, other item specific restrictions

        if (_roomObj.memories.contains(entry)) {
            // mark it as modified and update it in the room object ; we'll save it when we unload
            // the room
            entry.modified = true;
            _roomObj.updateMemories(entry);

        } else {
            // TODO: if this item does not yet have a memory, assign them a new memory id, update
            // their Item and FurniData records and store the new memory
        }
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
        _roomObj.setRoomService((RoomMarshaller) MsoyServer.invmgr.registerDispatcher(
                                    new RoomDispatcher(this), false));

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

        // TODO: load up any pets that are "let out" in this room
    }

    @Override // from PlaceManager
    protected void bodyUpdated (OccupantInfo info)
    {
        super.bodyUpdated(info);

        // TODO: if this occupant just disconnected, reassign their Pets
    }

    @Override // from PlaceManager
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // TODO: reassign this occupant's Pets
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        MsoyServer.invmgr.clearDispatcher(_roomObj.roomService);
        super.didShutdown();

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
                        MsoyServer.memoryRepo.updateMemories(memrecs);
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
        // automatically add the room to their recent list
        MemberObject memberObj = (MemberObject) body;
        memberObj.addToRecentScenes(_scene.getId(), _scene.getName());

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

    /** The room object. */
    protected RoomObject _roomObj;
}
