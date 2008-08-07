//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.TimerEvent;

import flash.utils.Timer;
import flash.utils.getTimer; // function import

import com.threerings.flash.AnimationImpl;

import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyScene;

/**
 * Handles moving an occupant sprite around in a scene.
 */
public class WalkAnimation extends AnimationImpl
{
    public function WalkAnimation (
        spr :OccupantSprite, scene :MsoyScene, src :MsoyLocation, dest :MsoyLocation)
    {
        _sprite = spr;
        _source = [ src.x, src.y, src.z, src.orient ];
        _dest = [ dest.x, dest.y, dest.z, dest.orient ];

        var dx :Number = scene.getWidth() * (dest.x - src.x);
        var dy :Number = scene.getHeight() * (dest.y - src.y);
        var dz :Number = scene.getDepth() * (dest.z - src.z);

        // calculate the duration- walk speed is specified in pixels/second.
        _duration = int(1000 * Math.sqrt((dx * dx) + (dy * dy) + (dz * dz)) / spr.getMoveSpeed());
    }

    /**
     * Update the actor's location based on the time elapsed.
     */
    override public function updateAnimation (elapsed :Number) :void
    {
        if (elapsed >= _duration) {
            // golly, we're done!
            _sprite.setLocation(_dest);
            _sprite.walkCompleted(_dest[3] as Number); // orient
            stopAnimation();
            return;
        }

        // otherwise calculate the intermediate location
        var current :Array = [];
        for (var ii :int = 0; ii < 3; ii++) { // don't do orient
            current[ii] = moveFunction(elapsed, _source[ii],
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
        return ((delta * stamp) / duration) + initial;
    }

    /** The sprite we'll be moving. */
    protected var _sprite :OccupantSprite;

    /** The source location. */
    protected var _source :Array;

    /** The destination location. */
    protected var _dest :Array;

    /** The amount of time we'll spend moving. */
    protected var _duration :int;
}
}
