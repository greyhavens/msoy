//
// $Id$

package com.threerings.msoy.room.client {

/**
 * Communicates between user avatar code and their sprite.
 */
public class AvatarBackend extends ActorBackend
{
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);
        o["setPreferredY_v1"] = setPreferredY_v1;
    }

    override protected function populateControlInitProperties (o :Object) :void
    {
        super.populateControlInitProperties(o);
        o["isSleeping"] = (_sprite as MemberSprite).isIdle();
    }

    /**
     * Called by user code to set a preferred height off the ground for their moves.
     */
    protected function setPreferredY_v1 (pixels :int) :void
    {
        (_sprite as MemberSprite).setPreferredYFromUser(pixels);
    }
}
}
