//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Pet;

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

    @Override // from SimpleStreamableObject
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", ownerId=").append(_ownerId);
    }

    @Override // from ActorInfo
    protected MediaDesc getStaticMedia ()
    {
        return Pet.getStaticImagePetMedia();
    }

    protected int _ownerId;
}
