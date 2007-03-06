package com.threerings.msoy.world.client {

import flash.events.TimerEvent;

import flash.utils.Timer;
import flash.utils.getTimer; // function import

import com.threerings.flash.Animation;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;

/**
 * Handles moving an ActorSprite around in a scene.
 */
public class WalkAnimation extends Animation
{
    public function WalkAnimation (
        spr :ActorSprite, scene :MsoyScene,
        src :MsoyLocation, dest :MsoyLocation)
    {
        super();

        _sprite = spr;
        _source = [ src.x, src.y, src.z, src.orient ];
        _dest = [ dest.x, dest.y, dest.z, dest.orient ];

        // TODO move/define magic numbers?
        // TODO: perhaps actors define their own duration and easing function?
        var dx :Number = scene.getWidth() * (dest.x - src.x);
        var dy :Number = 400 * (dest.y - src.y);
        var dz :Number = scene.getDepth() * (dest.z - src.z);
        _duration = int(2 * Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    /**
     * Update the actor's location based on the time elapsed.
     */
    override protected function enterFrame () :void
    {
        var currentTime :int = _now - _start;
        if (currentTime >= _duration) {
            // golly, we're done!
            _sprite.setLocation(_dest);
            _sprite.walkCompleted(_dest[3] as Number); // orient
            stop();
            return;
        }

        // otherwise calculate the intermediate location
        var current :Array = [];
        for (var ii :int = 0; ii < 3; ii++) { // don't do orient
            current[ii] = moveFunction(currentTime, _source[ii],
                _dest[ii] - _source[ii], _duration);
        }
        _sprite.setLocation(current);
    }

    /**
     * The easing function we use to move objects around the scene.
     */
    protected static function moveFunction (
        stamp :int, initial :Number, delta :Number, duration :int) :Number
    {
        return (delta * stamp) / duration + initial;
    }

    /** The sprite we'll be moving. */
    protected var _sprite :ActorSprite;

    /** The source location. */
    protected var _source :Array;

    /** The destination location. */
    protected var _dest :Array;

    /** The amount of time we'll spend moving. */
    protected var _duration :int;
}
}
