//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.data.PetObject;

/**
 * Contains published information on a pet in a scene.
 */
public class PetInfo extends ActorInfo
{
    /**
     * Creates an occupant info for the specified pet.
     */
    public PetInfo (PetObject petobj, boolean isFollowing)
    {
        super(petobj, petobj.pet.getFurniMedia(), petobj.pet.getIdent());
        _ownerId = petobj.pet.ownerId;
        _isFollowing = isFollowing;
    }

    /** Constructor used for unserialization. */
    public PetInfo ()
    {
    }

    /**
     * Returns the member id of this pet's owner.
     */
    public int getOwnerId ()
    {
        return _ownerId;
    }

    /**
     * Returns true if this pet is following its owner around the world, false if it is in the room
     * because it has been permanently "let out" in the room.
     */
    public boolean isFollowing ()
    {
        return _isFollowing;
    }

    protected int _ownerId;
    protected boolean _isFollowing;
}
