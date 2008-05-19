//
// $Id$

package com.threerings.msoy.world.server;

import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import com.samskivert.jdbc.WriteOnlyUnit;

import com.threerings.util.Name;

import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.whirled.client.SceneMoveAdapter;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.PetCodes;
import com.threerings.msoy.world.data.PetObject;

import static com.threerings.msoy.Log.log;

/**
 * Manages a Pet at runtime.
 */
public class PetHandler
{
    public PetHandler (PetManager petmgr, Pet pet, List<EntityMemoryEntry> memories)
    {
        _petobj = MsoyServer.omgr.registerObject(new PetObject());
        _petobj.memories = memories;
        _petobj.setUsername(new Name(pet.name));
        _petobj.pet = pet;
        _petmgr = petmgr;
        _petmgr.mapHandler(pet.itemId, this);
    }

    /**
     * Returns this pet's distributed object.
     */
    public PetObject getPetObject ()
    {
        return _petobj;
    }

    /**
     * Shuts down this pet, removing it from the world and cleaning up its handler.
     */
    public void shutdown (boolean roomDidShutdown)
    {
        // remove ourselves from the handler mapping
        _petmgr.clearHandler(_petobj.pet.itemId);

        // if we're not shutting down because our room shutdown...
        if (!roomDidShutdown) {
            // leave our current location (which will extract our memories)
            MsoyServer.screg.leaveOccupiedScene(_petobj);
            // and save them
            RoomManager.flushMemories(_petobj.memories);
        }

        // TODO: if we're following a member, clear that out?

        // finally, destroy our pet object
        MsoyServer.omgr.destroyObject(_petobj.getOid());
    }

    /**
     * Moves the pet into the specified room.
     */
    public void enterRoom (final int sceneId)
    {
        log.info("Entering room [pet=" + this + ", sceneId=" + sceneId + "].");

        // then enter the scene like a proper scene entity
        MsoyServer.screg.moveTo(_petobj, sceneId, Integer.MAX_VALUE, new SceneMoveAdapter() {
            public void requestFailed (String reason) {
                log.warning("Pet failed to enter scene [pet=" + this + ", scene=" + sceneId +
                            ", reason=" + reason + "].");
                shutdown(false);
            }
        });
    }

    /**
     * Places this pet in the owner's room and puts them in follow mode.
     */
    public void moveToOwner (MemberObject owner)
        throws InvocationException
    {
        validateOwnership(owner);
        // set ourselves to follow mode
        startFollowing(owner);
        // head to our destination
        enterRoom(owner.getSceneId());
    }

    /**
     * Handles an order from the specified user on this pet.
     */
    public void orderPet (MemberObject owner, int order)
        throws InvocationException
    {
        validateOwnership(owner);

        switch (order) {
        case Pet.ORDER_SLEEP:
            updateUsage(Item.UNUSED, 0);
            shutdown(false);
            break;

        case Pet.ORDER_FOLLOW:
            startFollowing(owner);
            break;

        case Pet.ORDER_GO_HOME:
            stopFollowing(owner);
            updateUsage(Item.USED_AS_PET, owner.homeSceneId);
            // TODO: if home room is resolved (on any server), instruct it to resolve pet
            shutdown(false);
            break;

        case Pet.ORDER_STAY:
            // make sure the requester is in a room that they own
            PlaceManager plmgr = MsoyServer.plreg.getPlaceManager(owner.getPlaceOid());
            if (!(plmgr instanceof RoomManager)) {
                log.info("Owner no longer in a room? [who=" + owner.who() + ", in=" + plmgr + "].");
                throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
            }
            ((RoomManager)plmgr).checkCanAddPet(owner);
            // stop following our owner
            stopFollowing(owner);
            // note that we want to autoload in this room
            updateUsage(Item.USED_AS_PET, ((RoomManager)plmgr).getScene().getId());
            break;

        default:
            log.warning("Received unknown pet order [from=" + owner.who() +
                        ", order=" + order + "].");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }
    }

    @Override // from Object
    public String toString ()
    {
        return "[id=" + _petobj.pet.itemId + ", name=" + _petobj.pet.name + "]";
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
            log.warning("Pet handling by non-owner [who=" + owner.who() + ", pet=" + this +
                        ", ownerId=" + _petobj.pet.ownerId + "].");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Sets up the necessary bits to follow our owner. If this is not possible, we'll throw an
     * invocation exception and shut ourselves down.
     */
    protected void startFollowing (MemberObject owner)
        throws InvocationException
    {
        if (_follist != null) {
            log.warning("Asked to follow but we're already following! [pet=" + this +
                        ", target=" + owner.who() + "].");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }

        if (owner.walkingId != 0) {
            shutdown(false);
            throw new InvocationException(PetCodes.E_ALREADY_WALKING);
        }

        owner.setWalkingId(_petobj.pet.itemId);
        owner.addListener(_follist = new ObjectDeathListener() {
            public void objectDestroyed (ObjectDestroyedEvent event) {
                // our followee logged off, shut ourselves down
                shutdown(false);
            }
        });
    }

    /**
     * Clears out our following bits.
     */
    protected void stopFollowing (MemberObject owner)
    {
        // make sure this member is walking us
        if (owner.walkingId != _petobj.pet.itemId) {
            log.warning("Requested to stop following member who's not walking us [pet=" + this +
                        ", stopper=" + owner.who() + ", walking=" + owner.walkingId + "].");
            return;
        }
        owner.setWalkingId(0);

        if (_follist != null) {
            owner.removeListener(_follist);
            _follist = null;
        }
    }

    /**
     * Update the marked usage and location of the pet we handle.
     */
    protected void updateUsage (final byte usageType, final int location)
    {
        final int itemId = _petobj.pet.itemId;
        MsoyServer.invoker.postUnit(new WriteOnlyUnit("updatePetUsage(" + itemId + ")") {
            public void invokePersist () throws Exception {
                MsoyServer.itemMan.getPetRepository().markItemUsage(
                    new int[] { itemId }, usageType, location);
            }
        });
    }

    protected PetManager _petmgr;
    protected PetObject _petobj;
    protected ObjectDeathListener _follist;
}
