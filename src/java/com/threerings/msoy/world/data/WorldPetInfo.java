//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.data.PetObject;

/**
 * Extends {@link WorldOccupantInfo} with pet-specific stuff.
 */
public class WorldPetInfo extends WorldActorInfo
{
    /** The member id of this pet's owner. */
    public int ownerId;

    /** Set to true if the pet is following its owner around the world, false if it is in this room
     * because it has been permanently "let out" in this room. */
    public boolean isFollowing;

    /** Constructor used for unserialization. */
    public WorldPetInfo ()
    {
    }

    /**
     * Creates an occupant info for the specified pet.
     */
    public WorldPetInfo (PetObject petobj, boolean isFollowing)
    {
        super(petobj, petobj.pet.getIdent(), petobj.pet.getFurniMedia());
        this.ownerId = petobj.pet.ownerId;
        this.isFollowing = isFollowing;
    }
}
