//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Pet;

/**
 * Contains published information on a pet in a scene.
 */
public class PetInfo extends ActorInfo
{
    /**
     * Creates an occupant info for the specified pet.
     */
    public PetInfo (PetObject petobj, boolean useStaticImage)
    {
        super(petobj, null, null);
        _ownerId = petobj.pet.ownerId;

        // configure the media
        if (useStaticImage) {
            _media = Pet.getStaticImagePetMedia();
            _ident = new ItemIdent(Item.OCCUPANT, petobj.getOid());
        } else {
            _media = petobj.pet.getFurniMedia();
            _ident = petobj.pet.getIdent();
        }
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
