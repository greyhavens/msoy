//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.crowd.data.BodyObject;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.data.ActorInfo;
import com.threerings.msoy.data.PetObject;

/**
 * Contains extra information for an occupant when they are in the virtual world.
 */
public class WorldActorInfo extends ActorInfo
    implements WorldOccupantInfo
{
    /** Used for unserialization. */
    public WorldActorInfo ()
    {
    }

    /**
     * Creates a world actor info record for the specified actor. (TODO: create MobObject version,
     * nix this version).
     */
    public WorldActorInfo (BodyObject body, ItemIdent ident, MediaDesc media)
    {
        super(body, ident);
        _media = media;
    }

    /**
     * Creates a world actor info record for the supplied pet.
     */
    public WorldActorInfo (PetObject petobj)
    {
        super(petobj, petobj.pet.getIdent());
        _media = petobj.pet.getFurniMedia();
    }

    // from interface WorldOccupantInfo
    public MediaDesc getMedia ()
    {
        return _media;
    }

    /** The media that represents this occupant. */
    protected MediaDesc _media;
}
