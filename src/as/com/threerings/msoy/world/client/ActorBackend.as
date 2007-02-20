package com.threerings.msoy.world.client {

public class ActorBackend extends EntityBackend
{
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["setLocation_v1"] = setLocation_v1;
        o["setOrientation_v1"] = setOrientation_v1;
    }

    /**
     * Called by user code when it wants to change the actor's scene location.
     */
    protected function setLocation_v1 (x :Number, y :Number, z: Number, orient :Number) :void
    {
        (_sprite as ActorSprite).setLocationFromUser(x, y, z, orient);
    }

    /**
     * Called by user code when it wants to change the actor's scene orientation.
     */
    protected function setOrientation_v1 (orient :Number) :void
    {
        (_sprite as ActorSprite).setOrientationFromUser(orient);
    }
}
}
