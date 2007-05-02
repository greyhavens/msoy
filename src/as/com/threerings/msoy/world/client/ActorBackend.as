package com.threerings.msoy.world.client {

import com.threerings.msoy.world.data.MsoyLocation;

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
        o["getRoomBounds_v1"] = getRoomBounds_v1;

        // oldness (used for a very short time), deprecated 2007-04-24
        o["setWalkSpeed_v1"] = function (num :Number) :void {
            setMoveSpeed_v1(num * 1000);
        };
    }

    override protected function populateControlInitProperties (o :Object) :void
    {
        super.populateControlInitProperties(o);

        var sprite :ActorSprite = (_sprite as ActorSprite);
        var loc :MsoyLocation = sprite.getLocation();
        o["location"] = [ loc.x, loc.y, loc.z ];
        o["orient"] = loc.orient;
        o["isMoving"] = sprite.isMoving();
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
     * Called by user code to determine the current room's boundaries.
     */
    protected function getRoomBounds_v1 () :Array
    {
        return (_sprite as ActorSprite).getRoomBounds();
    }

    /**
     * Called by user code when it wants to change the actor's state.
     */
    protected function setState_v1 (state :String) :void
    {
        if (state != null && state.length > 64) {
            throw new ArgumentError("States may only be a maximum of 64 characters");
        }
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
