//
// $Id$

package com.threerings.msoy.world.server;

import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.chat.data.ChatChannel;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.RoomName;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.server.persist.PetRecord;
import com.threerings.msoy.item.data.all.Pet;

import com.threerings.msoy.world.client.PetService;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.PetCodes;
import com.threerings.msoy.world.data.PetInfo;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.server.persist.MemoryRecord;

import static com.threerings.msoy.Log.log;

/**
 * Takes care of loading, unloading and handling of Pets.
 */
public class PetManager
    implements PetProvider
{
    /**
     * Initializes the pet manager and prepares it for operation.
     */
    public void init (InvocationManager invmgr)
    {
        // register our pet invocation services
        invmgr.registerDispatcher(new PetDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    /**
     * Loads up all pets that are "let out" in the specified room. Any pets that live in this room
     * but are currently being walked will not be added to the room (they are already extant in the
     * world).
     */
    public void loadRoomPets (final RoomObject roomObj, final int sceneId)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("loadRoomPets(" + sceneId + ")") {
            public void invokePersist () throws Exception {
                // load up our pets, collect their memory ids and convert them to runtime objs
                ArrayIntSet mids = new ArrayIntSet();
                for (PetRecord petrec :
                         MsoyServer.itemMan.getPetRepository().loadItemsByLocation(sceneId)) {
                    _pets.add((Pet)petrec.toItem());
                    mids.add(petrec.itemId);
                }

                // next load up their memories
                if (mids.size() > 0) {
                    for (MemoryRecord memrec : MsoyServer.memoryRepo.loadMemories(Pet.PET, mids)) {
                        List<EntityMemoryEntry> mems = _memories.get(memrec.itemId);
                        if (mems == null) {
                            _memories.put(memrec.itemId, mems = Lists.newArrayList());
                        }
                        mems.add(memrec.toEntry());
                    }
                }
            }

            public void handleSuccess () {
                resolveRoomPets(sceneId, roomObj, _pets, _memories);
            }
            public void handleFailure (Exception e) {
                log.log(Level.WARNING, "Failed to load pets [scene=" + sceneId + "].", e);
            }

            protected List<Pet> _pets = Lists.newArrayList();
            protected IntMap<List<EntityMemoryEntry>> _memories = IntMaps.newHashIntMap();
        });
    }

    /**
     * Called when a room shuts down, unloads any pets that are "let out" in that room.
     */
    public void shutdownRoomPets (final RoomObject roomObj)
    {
        for (OccupantInfo info : roomObj.occupantInfo) {
            if (info instanceof PetInfo) {
                PetHandler handler = _handlers.get(((PetInfo)info).getItemIdent().itemId);
                if (handler == null) {
                    log.warning("Failed to locate handler for pet in shutdown room " +
                                "[room=" + roomObj.getOid() + ", oinfo=" + info + "].");
                } else {
                    handler.shutdown(true);
                }
            }
        }
    }

    // from interface PetProvider
    public void callPet (ClientObject caller, final int petId,
                         final PetService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject)caller;

        // make sure the requester is in a room that they own
        PlaceManager plmgr = MsoyServer.plreg.getPlaceManager(user.getPlaceOid());
        if (!(plmgr instanceof RoomManager)) {
            log.info("Owner no longer in a room? [who=" + user.who() + ", in=" + plmgr + "].");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }
        ((RoomManager)plmgr).checkCanAddPet(user);

        // now check to see if the pet is already loaded
        PetHandler handler = _handlers.get(petId);
        if (handler != null) {
            // moveToOwner may throw an InvocationException if this isn't the owner..
            handler.moveToOwner(user, null);
            listener.requestProcessed();
            return;
        }

        // resolve the pet, then move them to their owner
        MsoyServer.invoker.postUnit(new RepositoryUnit("callPet(" + petId + ")") {
            public void invokePersist () throws Exception {
                // load up the pet's record
                PetRecord petrec = MsoyServer.itemMan.getPetRepository().loadItem(petId);
                if (petrec == null) {
                    throw new Exception("callPet() on non-existent pet");
                }
                if (petrec.ownerId != user.getMemberId()) {
                    throw new Exception("Pet handling by non-owner [who=" + user.who() + "].");
                }
                _pet = (Pet)petrec.toItem();

                // load up its memory
                for (MemoryRecord memrec : MsoyServer.memoryRepo.loadMemory(Pet.PET, petId)) {
                    _memory.add(memrec.toEntry());
                }
            }

            public void handleSuccess () {
                resolvePet(user, _pet, _memory);
                listener.requestProcessed();
            }
            public void handleFailure (Exception e) {
                log.log(Level.WARNING, "Failed to load pet [pet=" + petId + "].", e);
                listener.requestFailed(PetCodes.E_INTERNAL_ERROR);
            }

            protected Pet _pet;
            protected List<EntityMemoryEntry> _memory = Lists.newArrayList();
        });
    }

    // from interface PetProvider
    public void orderPet (ClientObject caller, int petId, int order,
                          PetService.ConfirmListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject)caller;
        PetHandler handler = _handlers.get(petId);
        if (handler == null) {
            log.warning("orderPet() on non-resolved pet [who=" + user.who() +
                        ", pet=" + petId + "].");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }

        // pass the buck on to the handler (if it does not throw an invocation exception, then
        // we'll assume the order was processed)
        handler.orderPet(user, order);
        listener.requestProcessed();
    }

    // from interface PetProvider
    public void sendChat (ClientObject caller, int bodyOid, int sceneId, String message,
                          PetService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject)caller;

        // get the manager of the room where we're chatting
        PlaceManager pmgr = MsoyServer.plreg.getPlaceManager(user.getPlaceOid());
        if (!(pmgr instanceof RoomManager)) {
            log.warning("sendChat() on invalid location [location=" + user.location + "]");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }

        // are we in the right scene?
        RoomManager mgr = (RoomManager) pmgr;
        if (mgr.getScene().getId() != sceneId) {
            log.warning("sendChat() requested during a room change; chat will be ignored.");

        } else {
            // get occupant info, make sure the given oid is a pet in the room!
            OccupantInfo info = mgr.getOccupantInfo(bodyOid);
            if (!(info instanceof PetInfo)) {
                log.warning("sendChat() on invalid occupant [bodyOid=" + bodyOid +
                            ", loc=" + user.location);
                throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
            }

            // check if the user actually owns the pet
            PetInfo petInfo = (PetInfo)info;
            if (!mgr.ensureEntityControl(user, petInfo.getItemIdent(), "PetManager.sendChat")) {
                log.warning("sendChat() requested by non-owner [who=" + user.who() +
                            ", pet=" + petInfo + "].");
                throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
            }

            // it's in the room, let's chat
            MsoyScene scene = (MsoyScene) mgr.getScene();
            ChatChannel channel = ChatChannel.makeRoomChannel(
                new RoomName(scene.getName(), scene.getId()));
            MsoyServer.channelMan.forwardSpeak(
                caller, petInfo.username, channel, message, ChatCodes.DEFAULT_MODE, listener);
            return;
        }

        listener.requestProcessed();
    }


    /**
     * Called after {@link #loadRoomPets} has loaded our persistent pet data to finish the job of
     * pet resolution.
     */
    protected void resolveRoomPets (int sceneId, RoomObject roomObj, List<Pet> pets,
                                    IntMap<List<EntityMemoryEntry>> memories)
    {
        for (Pet pet : pets) {
            // if this pet is already resolved (is wandering around with its owner), skip it (TODO:
            // this becomes more complicated when our pet can wander to other servers)
            if (_handlers.containsKey(pet.itemId)) {
                continue;
            }

            // create a handler for this pet and start them in this room
            PetHandler handler = new PetHandler(this, pet);
            handler.enterRoom(sceneId, roomObj, memories.get(pet.itemId));
        }
    }

    /**
     * Finishes the resolution of a pet initiated by {@link #callPet}.
     */
    protected void resolvePet (MemberObject owner, Pet pet, List<EntityMemoryEntry> memory)
    {
        // instead of doing a bunch of complicated prevention to avoid multiply resolving pets,
        // we'll just get this far and abandon ship; it's not going to happen that often
        if (_handlers.containsKey(pet.itemId)) {
            log.info("resolvePet() ignoring repeat resolution request. [pet=" + pet.itemId + "].");
            return;
        }

        // create a handler for this pet (which will register itself with us); then direct it
        // immediately to the room occupied by its owner
        PetHandler handler = new PetHandler(this, pet);
        try {
            handler.moveToOwner(owner, memory);
        } catch (InvocationException ie) {
            log.warning("Newly resolved pet rejects its owner? [error=" + ie + "].");
            // wow, this can only mean that the owner is not our owner.. this shouldn't happen
            // but let's try doing the right thing.
            handler.shutdown(false);
        }
    }

    protected void mapHandler (int itemId, PetHandler handler)
    {
        _handlers.put(itemId, handler);
    }

    protected void clearHandler (int itemId)
    {
        _handlers.remove(itemId);
    }

    /** Maintains a mapping of all pet handlers by item id. */
    protected IntMap<PetHandler> _handlers = IntMaps.newHashIntMap();
}
