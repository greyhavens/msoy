//
// $Id$

package com.threerings.msoy.world.server;

import java.util.ArrayList;

import com.samskivert.util.StringUtil;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.client.SceneMoveAdapter;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PetObject;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.PetCodes;
import com.threerings.msoy.world.data.RoomObject;

import static com.threerings.msoy.Log.log;

/**
 * Manages a Pet at runtime.
 */
public class PetHandler
{
    public PetHandler (PetManager petmgr, Pet pet)
    {
        _petobj = MsoyServer.omgr.registerObject(new PetObject());
        _petobj.setUsername(new Name(pet.name));
        _petobj.pet = pet;
        _petmgr = petmgr;
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
    public void enterRoom (final int sceneId, RoomObject roomObj, ArrayList<MemoryEntry> memories)
    {
        // add our memories to the room
        _roomObj = roomObj;
        try {
            _roomObj.startTransaction();
            if (memories != null) {
                for (MemoryEntry entry : memories) {
                    _roomObj.addToMemories(entry);
                }
            }
        } finally {
            _roomObj.commitTransaction();
        }

        // then enter the scene like a proper scene entity
        MsoyServer.screg.sceneprov.moveTo(_petobj, sceneId, -1, new SceneMoveAdapter() {
            public void requestFailed (String reason) {
                log.warning("Pet failed to enter scene [pet=" + _petobj.pet + ", scene=" + sceneId +
                            ", reason=" + reason + "].");
                // TODO: shutdown? freakout? call the Elite Beat Agents?
            }
        });
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

        // now remove those from the room
        try {
            _roomObj.startTransaction();
            for (MemoryEntry entry : memories) {
                _roomObj.removeFromMemories(entry.getKey());
            }
        } finally {
            _roomObj.commitTransaction();
        }

        // clear out, clear out and clear out
        _roomObj = null;
        MsoyServer.screg.sceneprov.leaveOccupiedScene(_petobj);
        return memories;
    }

    /**
     * Places this pet in follow mode and moves them to the owner's room.
     *
     * @param memory if the pet was just resolved, this should contain its memory. Otherwise the
     * pet will assume it is in a room already and will extract its memory from its current room
     * before moving.
     */
    public void moveToOwner (MemberObject owner, ArrayList<MemoryEntry> memory)
        throws InvocationException
    {
        validateOwnership(owner);

        // locate the room to which we are headed
        DObject dobj = MsoyServer.omgr.getObject(owner.location);
        if (!(dobj instanceof RoomObject)) {
            log.warning("moveToOwner() found owner in non-RoomObject [pet=" + _petobj.pet +
                        ", owner=" + owner.who() +
                        ", location=" + StringUtil.shortClassName(dobj) + "].");
            if (_roomObj == null) { // stay in the room we're in or unload if we're not in a room
                shutdown(false);
            }
            return;
        }

        // leave any room we currently occupy
        if (_roomObj != null) {
            if (memory != null) {
                log.warning("moveToOwner() provided with memory but we're already in a room " +
                            "[pet=" + _petobj.pet + ", owner=" + owner.who() + "].");
                // fall through and ignore the memory supplied by the caller
            }
            memory = leaveRoom();
        }

        // set ourselves to follow mode
        _petobj.setFollowId(owner.getMemberId());

        // head to our destination
        enterRoom(owner.sceneId, (RoomObject)dobj, memory);
    }

    /**
     * Handles an order from the specified user on this pet.
     */
    public void orderPet (MemberObject orderer, int order)
        throws InvocationException
    {
        validateOwnership(orderer);

        switch (order) {
        case Pet.ORDER_SLEEP:
            shutdown(false);
            break;

        default:
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR); // TODO
        }
    }

    /**
     * Validate that the specified user is the owner of this pet.
     */
    protected void validateOwnership (MemberObject owner)
        throws InvocationException
    {
        // TODO: if pet is updated or changes hands, we need to update the resolved PetObject
        if (_petobj.pet.ownerId != owner.getMemberId()) {
            // TODO: allow support personnel?
            log.warning("Pet handling by non-owner [who=" + owner.who() +
                        ", petId=" + _petobj.pet.itemId +
                        ", ownerId=" + _petobj.pet.ownerId + "].");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }
    }

    protected PetManager _petmgr;
    protected PetObject _petobj;
    protected RoomObject _roomObj;
}
