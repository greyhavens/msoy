//
// $Id$

package com.threerings.msoy.world.server;

import java.util.ArrayList;

import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PetObject;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.PetCodes;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.WorldPetInfo;

/**
 * Manages a Pet at runtime.
 */
public class PetHandler
{
    public PetHandler (PetManager petmgr, Pet pet)
    {
        _petobj = MsoyServer.omgr.registerObject(new PetObject());
        _petobj.pet = pet;
        _petmgr.mapHandler(pet.itemId, this);
    }

    /**
     * Shuts down this pet, removing it from the world and cleaning up its handler.
     */
    public void shutdown (boolean roomDidShutdown)
    {
        // remove ourselves from the handler mapping
        _petmgr.clearHandler(_petobj.pet.itemId);

        // if we're not shutting down because our room shutdown...
        if (!roomDidShutdown && _roomObj != null) {
            // we need to leave the room (and extract our memory from it)
            ArrayList<MemoryEntry> memories = leaveRoom();
            // TODO: save any modified memory entries
        }

        // finally, destroy our pet object
        MsoyServer.omgr.destroyObject(_petobj.getOid());
    }

    /**
     * Enters the pet into the supplied room. The supplied memory should come from having loaded
     * the pet for the first time or from extracting it from the room the pet just left.
     */
    public void enterRoom (RoomObject roomObj, ArrayList<MemoryEntry> memories)
    {
        _roomObj = roomObj;
        try {
            _roomObj.startTransaction();
            _roomObj.addToOccupantInfo(new WorldPetInfo(_petobj, false));
            if (memories != null) {
                for (MemoryEntry entry : memories) {
                    _roomObj.addToMemories(entry);
                }
            }
        } finally {
            _roomObj.commitTransaction();
        }
    }

    /**
     * Removes the pet from its current room, extracting and returning its active memory.
     */
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

    /**
     * Places this pet in follow mode and moves them to the owner's room.
     *
     * @param memory if the pet was just resolved, this will contain its memory. Otherwise the pet
     * will assume it is in a room already and will extract its memory from its current room before
     * moving.
     */
    public void moveToOwner (MemberObject owner, ArrayList<MemoryEntry> memory)
    {
        // TODO
    }

    /**
     * Handles an order from the specified user on this pet.
     */
    public void orderPet (MemberObject orderer, int order)
        throws InvocationException
    {
        throw new InvocationException(PetCodes.E_INTERNAL_ERROR); // TODO
    }

    protected PetManager _petmgr;
    protected PetObject _petobj;
    protected RoomObject _roomObj;
}
