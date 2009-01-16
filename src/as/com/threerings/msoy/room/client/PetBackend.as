//
// $Id$

package com.threerings.msoy.room.client {

public class PetBackend extends ActorBackend
{
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["sendChatMessage_v1"] = sendChatMessage_v1;

        // old properties, backwards compatibility
        o["getName_v1"] = getName_v1;
    }

    /**
     * Called by user code to send a chat message to the room.
     */
    protected function sendChatMessage_v1 (msg :String) :void
    {
        if (_sprite != null) {
            (_sprite as PetSprite).sendChatMessage(msg);
        }
    }

    /**
     * Deprecated on 2008-07-18. Use getEntityProperty(PROP_NAME) instead.
     */
    protected function getName_v1 () :String
    {
        return getEntityProperty_v1(null, "std:name") as String;
    }
}
}
