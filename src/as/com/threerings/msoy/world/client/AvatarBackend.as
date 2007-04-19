package com.threerings.msoy.world.client {

public class AvatarBackend extends ActorBackend
{
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["setPreferredY_v1"] = setPreferredY_v1;
    }

    /**
     * Called by user code to set a preferred height off the ground for their
     * moves.
     */
    protected function setPreferredY_v1 (pixels :int) :void
    {
        (_sprite as AvatarSprite).setPreferredYFromUser(pixels);
    }
}
}
