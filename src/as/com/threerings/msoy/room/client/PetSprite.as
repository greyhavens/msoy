//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.util.CommandEvent;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.PetInfo;

/**
 * Extends {@link ActorSprite} with pet-specific stuff.
 */
public class PetSprite extends ActorSprite
{
    public function PetSprite (ctx :WorldContext, occInfo :PetInfo, extraInfo :Object)
    {
        super(ctx, occInfo, extraInfo);
    }

    /**
     * This function sends a chat message to the entire room. Called by our backend
     * in response to a request from usercode.
     */
    public function sendChatMessage (msg :String) :void
    {
        var ctrl :RoomController = getController(true);
        if (ctrl != null) {
            ctrl.sendPetChatMessage(msg, getActorInfo());
        }
    }

    // from ActorSprite
    override public function getDesc () :String
    {
        return "m.pet";
    }

    // from MsoySprite
    override public function getHoverColor () :uint
    {
        return PET_HOVER;
    }

    // from MsoySprite
    override public function hasAction () :Boolean
    {
        return true;
    }

    // from ActorSprite
    override public function toString () :String
    {
        return "PetSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }

    // from MsoySprite
    override protected function postClickAction () :void
    {
        CommandEvent.dispatch(this, RoomController.PET_CLICKED, this);
    }

    override protected function getSpecialProperty (name :String) :Object
    {
        switch (name) {
        case "member_id":
            return (_occInfo as PetInfo).getOwnerId();

        default:
            return super.getSpecialProperty(name);
        }
    }

    // from ActorSprite
    override protected function createBackend () :EntityBackend
    {
        return new PetBackend();
    }

    // from OccupantSprite
    override protected function createNameField () :NameField
    {
        return new NameField(true);
    }
}
}
