//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.util.CommandEvent;

import com.threerings.msoy.world.data.PetInfo;

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
        if (_ident != null && (parent is RoomView)) {
            (parent as RoomView).getRoomController().sendPetChatMessage(msg, getActorInfo());
        }
    }

    /**
     * Receives a chat message from the room, and forwards it over to user land.
     */
    public function processChatMessage (msg :UserMessage) :void
    {
        callUserCode("receivedChat_v1", msg.getSpeakerDisplayName().toString(), msg.message);
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
