//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.util.CommandEvent;

import com.threerings.msoy.room.data.PetInfo;

/**
 * Extends {@link ActorSprite} with pet-specific stuff.
 */
public class PetSprite extends ActorSprite
{
    public function PetSprite (ctx :WorldContext, occInfo :PetInfo)
    {
        super(ctx, occInfo);
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

    /**
     * Receives a chat message from the room, and forwards it over to user land.
     */
    public function processChatMessage (
        fromEntityIdent :String, fromEntityName :String, msg :String) :void
    {
        if (hasUserCode("receivedChat_v2")) {
            callUserCode("receivedChat_v2", fromEntityIdent, msg);
        } else {
            callUserCode("receivedChat_v1", fromEntityName, msg);
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
