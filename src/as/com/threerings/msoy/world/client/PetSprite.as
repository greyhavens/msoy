//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

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

    override public function mouseClick (event :MouseEvent) :void
    {
        // let's just post a command to our controller
        CommandEvent.dispatch(this, RoomController.PET_CLICKED, this);
    }

    override public function toString () :String
    {
        return "PetSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }

//     override protected function createBackend () :EntityBackend
//     {
//         return new PetBackend();
//     }
}
}
