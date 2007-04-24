//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.CommandEvent;

import com.threerings.msoy.data.ActorInfo;

/**
 * Extends {@link ActorSprite} with pet-specific stuff.
 */
public class PetSprite extends ActorSprite
{
    public function PetSprite (occInfo :ActorInfo)
    {
        super(occInfo);
    }

    override public function getDesc () :String
    {
        return "m.pet";
    }

    override public function getHoverColor () :uint
    {
        return PET_HOVER;
    }

    override public function hasAction () :Boolean
    {
        return true;
    }

    override public function toString () :String
    {
        return "PetSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }

    override protected function getStatusColor (status :int) :uint
    {
        // all pets are white-named
        return 0xFFFFFF;
    }

    override protected function postClickAction () :void
    {
        CommandEvent.dispatch(this, RoomController.PET_CLICKED, this);
    }

//     override protected function createBackend () :EntityBackend
//     {
//         return new PetBackend();
//     }
}
}
