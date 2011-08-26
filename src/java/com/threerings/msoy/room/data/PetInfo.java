//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.data.MsoyBodyObject;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.data.all.Pet;

/**
 * Contains published information on a pet in a scene.
 */
@com.threerings.util.ActionScript(omit=true)
public class PetInfo extends ActorInfo
{
    /**
     * Creates an occupant info for the specified pet.
     */
    public PetInfo (PetObject petobj)
    {
        super(petobj);
    }

    /** Constructor used for unserialization. */
    public PetInfo ()
    {
    }

//    /**
//     * Returns the member id of this pet's owner.
//     */
//    public int getOwnerId ()
//    {
//        return ((PetName) username).getOwnerId();
//    }

    @Override // from ActorInfo
    protected void useStaticMedia ()
    {
        _media = Pet.getStaticImagePetMedia();
        _ident = new ItemIdent(MsoyItemType.OCCUPANT, getBodyOid());
    }

    @Override // from ActorInfo
    protected void useDynamicMedia (MsoyBodyObject body)
    {
        PetObject petobj = (PetObject)body;
        _media = petobj.pet.getFurniMedia();
        _ident = petobj.pet.getIdent();
    }
}
