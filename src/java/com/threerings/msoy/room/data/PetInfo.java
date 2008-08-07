//
// $Id$

package com.threerings.msoy.room.data;

/**
 * Contains published information on a pet in a scene.
 */
public class PetInfo extends ActorInfo
{
    /**
     * Creates an occupant info for the specified pet.
     */
    public PetInfo (PetObject petobj)
    {
        super(petobj, petobj.pet.getFurniMedia(), petobj.pet.getIdent());
        _ownerId = petobj.pet.ownerId;
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

    protected int _ownerId;
}
