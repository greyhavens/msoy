//
// $Id$

package com.threerings.msoy.room.client {

public class ActorBackend extends EntityBackend
{
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["setMoveSpeed_v1"] = setMoveSpeed_v1;
        o["setLocation_v1"] = setLocation_v1;
        o["setOrientation_v1"] = setOrientation_v1;
        o["setState_v1"] = setState_v1;
        o["getState_v1"] = getState_v1;

        // oldness (used for a very short time), deprecated 2007-04-24
        o["setWalkSpeed_v1"] = function (num :Number) :void {
            setMoveSpeed_v1(num * 1000);
        };
    }

    override protected function populateControlInitProperties (o :Object) :void
    {
        super.populateControlInitProperties(o);

        o["orient"] = _sprite.getLocation().orient;
        o["isMoving"] = (_sprite as ActorSprite).isMoving();
    }

    /**
     * Called by user code to configure the default move speed for this actor.
     */
    protected function setMoveSpeed_v1 (pixelsPerSecond :Number) :void
    {
        (_sprite as ActorSprite).setMoveSpeedFromUser(pixelsPerSecond);
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

    /**
     * Called by user code when it wants to change the actor's state.
     */
    protected function setState_v1 (state :String) :void
    {
        validateKeyName(state);
        (_sprite as ActorSprite).setState(state);
    }

    /**
     * Called by user code when it wants to inquire about the actor's state.
     */
    protected function getState_v1 () :String
    {
        return (_sprite as ActorSprite).getState();
    }
}
}
