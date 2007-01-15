//
// $Id$

package com.threerings.msoy.world.server;

import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;

import com.threerings.msoy.data.PetObject;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.server.persist.PetRecord;
import com.threerings.msoy.world.server.persist.MemoryRecord;

import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.WorldActorInfo;

import static com.threerings.msoy.Log.log;

/**
 * Takes care of loading, unloading and handling of Pets.
 */
public class PetManager
{
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
                for (MemoryRecord memrec : MsoyServer.memoryRepo.loadMemories(Pet.PET, mids)) {
                    ArrayList<MemoryEntry> mems = _memories.get(memrec.itemId);
                    if (mems == null) {
                        _memories.put(memrec.itemId, mems = new ArrayList<MemoryEntry>());
                    }
                    mems.add(memrec.toEntry());
                }
            }

            public void handleSuccess () {
                resolveRoomPets(roomObj, _pets, _memories);
            }
            public void handleFailure (Exception e) {
                log.log(Level.WARNING, "Failed to load pets [scene=" + sceneId + "].", e);
            }

            protected ArrayList<Pet> _pets = new ArrayList<Pet>();
            protected HashIntMap<ArrayList<MemoryEntry>> _memories =
                new HashIntMap<ArrayList<MemoryEntry>>();
        });
    }

    /**
     * Called when a room shuts down, unloads any pets that are "let out" in that room.
     */
    public void shutdownRoomPets (final RoomObject roomObj)
    {
        // TODO
    }

    /**
     * Called after {@link #loadRoomPets} has loaded our persistent pet data to finish the job of
     * pet resolution.
     */
    protected void resolveRoomPets (RoomObject roomObj, ArrayList<Pet> pets,
                                    HashIntMap<ArrayList<MemoryEntry>> memories)
    {
        // create PetObject records and handlers for each pet
        for (Pet pet : pets) {
            // if this pet is already resolved (is wandering around with its owner), skip it
            if (_respets.containsKey(pet.itemId)) {
                continue;
            }

            // create and register a pet distributed object
            PetObject petobj = MsoyServer.omgr.registerObject(new PetObject());
            petobj.pet = pet;
            _respets.put(pet.itemId, petobj);

            // create a handler for this pet and start them in this room
            PetHandler handler = new PetHandler(petobj);
            handler.enterRoom(roomObj, memories.get(pet.itemId));
        }
    }

    protected class PetHandler
    {
        public PetHandler (PetObject petobj)
        {
            _petobj = petobj;
        }

        public void enterRoom (RoomObject roomObj, ArrayList<MemoryEntry> memories)
        {
            _roomObj = roomObj;
            try {
                _roomObj.startTransaction();
                _roomObj.addToOccupantInfo(new WorldActorInfo(_petobj));
                if (memories != null) {
                    for (MemoryEntry entry : memories) {
                        _roomObj.addToMemories(entry);
                    }
                }
            } finally {
                _roomObj.commitTransaction();
            }
        }

        public ArrayList<MemoryEntry> leaveRoom ()
        {
            if (_roomObj == null) {
                return null; // NOOP!
            }

            // collect up our memory entries from our previous room
            ItemIdent petid = _petobj.pet.getIdent();
            ArrayList<MemoryEntry> memories = new ArrayList<MemoryEntry>();
            for (MemoryEntry entry : _roomObj.memories) {
                if (entry.item.equals(petid)) {
                    memories.add(entry);
                }
            }

            // now remove those and our occupant info from the room
            try {
                _roomObj.startTransaction();
                _roomObj.removeFromOccupantInfo(_petobj.getOid());
                for (MemoryEntry entry : memories) {
                    _roomObj.removeFromMemories(entry.getKey());
                }
            } finally {
                _roomObj.commitTransaction();
            }

            // clear out and clear out
            _roomObj = null;
            return memories;
        }

        protected PetObject _petobj;
        protected WorldActorInfo _info;
        protected RoomObject _roomObj;
    }

    /** Maintains a mapping of all resolves pets by item id. */
    protected HashIntMap<PetObject> _respets = new HashIntMap<PetObject>();
}
