//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.msoy.world.data.MsoyLocation;

public class PetBackend extends ActorBackend
{
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["sendChatMessage_v1"] = sendChatMessage_v1;
        o["getName_v1"] = getName_v1;
    }

    /**
     * Called by user code to send a chat message to the room.
     */
    protected function sendChatMessage_v1 (msg :String) :void
    {
        (_sprite as PetSprite).sendChatMessage(msg);
    }

    /**
     * Called by user code to get the name of this pet.
     */
    protected function getName_v1 () :String
    {
        return (_sprite as PetSprite).getOccupantInfo().username.toString();
    }
}
}
