package com.threerings.msoy.world.client {

import flash.events.TimerEvent;

import flash.utils.Timer;
import flash.utils.getTimer; // function import

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;

/**
 * Handles moving an ActorSprite around in a scene.
 */
public class SceneMover
{
    public function SceneMover (
        spr :ActorSprite, scene :MsoyScene,
        src :MsoyLocation, dest :MsoyLocation)
    {
        _sprite = spr;
        _source = [ src.x, src.y, src.z, src.orient ];
        _dest = [ dest.x, dest.y, dest.z, dest.orient ];

        // TODO move/define magic numbers?
        // TODO: perhaps actors define their own duration and easing function?
        var dx :Number = scene.getWidth() * (dest.x - src.x);
        var dy :Number = 400 * (dest.y - src.y);
        var dz :Number = scene.getDepth() * (dest.z - src.z);
        _duration = 2 * Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Start the move.
     */
    public function start () :void
    {
        addMover(this);
    }

    /**
     * Cancel the move before it's finished.
     */
    public function cancel () :void
    {
        removeMover(this);
        // do nothing else
    }

    /**
     * Update the actor's location based on the time elapsed.
     */
    protected function doInterval () :void
    {
        var currentTime :Number = _intervalTime - _startTime;
        if (currentTime >= _duration) {
            // golly, we're done!
            _sprite.setLocation(_dest);
            _sprite.moveCompleted(_dest[3] as Number); // orient
            removeMover(this);
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

    /** The sprite we'll be moving. */
    protected var _sprite :ActorSprite;

    /** The source location. */
    protected var _source :Array;

    /** The destination location. */
    protected var _dest :Array;

    /** The amount of time we'll spend moving. */
    protected var _duration :Number;

    /** The time at which this event started. */
    protected var _startTime :Number;

    // END: instance methods and variables
    //======================================================================
    // START: internal static functionality

    /**
     * Add the specified mover to the list of active movers.
     */
    protected static function addMover (mover :SceneMover) :void
    {
        // add it
        _movers.push(mover);

        // set up the timer
        if (!_timer) {
            _timer = new Timer(10); // 10ms: will be limited by frame rate
            _timer.addEventListener(TimerEvent.TIMER, handleTimer);
        }
        _timer.start();

        // set up the start
        if (isNaN(_intervalTime)) {
            _intervalTime = getTimer();
        }

        mover._startTime = _intervalTime;
    }

    /**
     * Remove the specified mover.
     */
    protected static function removeMover (mover :SceneMover) :void
    {
        var dex :int = _movers.indexOf(mover);
        if (dex != -1) {
            _movers.splice(dex, 1);

        } else {
            Log.getLog(SceneMover).warning("Removing unknown Mover: " + mover);
        }

        if (_movers.length == 0) {
            _intervalTime = NaN;
            _timer.reset();
        }
    }

    /**
     * Handle our timer event.
     */
    protected static function handleTimer (event :TimerEvent) :void
    {
        _intervalTime = getTimer();
        for (var ii :int = _movers.length - 1; ii >= 0; ii--) {
            SceneMover(_movers[ii]).doInterval();
        }

        // and instruct the flash player to update the display list (now!)
        event.updateAfterEvent();
    }

    /**
     * The easing function we use to move objects around the scene.
     */
    protected static function moveFunction (
        stamp :Number, initial :Number, delta :Number, duration :Number) :Number
    {
        return (delta * stamp) / duration + initial;
    }

    /** The global timer for all SceneMovers. */
    protected static var _timer :Timer;

    /** The currently active SceneMovers. */
    protected static var _movers :Array = [];

    /** The time at which the last event was fired. */
    protected static var _intervalTime :Number = NaN;
}
}
