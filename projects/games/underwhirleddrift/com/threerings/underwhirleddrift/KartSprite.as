package com.threerings.underwhirleddrift {

import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Matrix;

import flash.utils.getTimer;

import mx.core.MovieClipAsset;

[Event(name="crossedFinishLine", type="KartEvent")]
[Event(name="bonus", type="KartEvent")]
[Event(name="removeBonus", type="KartEvent")]
[Event(name="shield", type="KartEvent")]
[Event(name="fireball", type="KartEvent")]

public class KartSprite extends Sprite 
{
    public static const KART_LIGHT :String = "LightKart";
    public static const KART_MEDIUM :String = "MediumKart";
    public static const KART_HEAVY :String = "HeavyKart";

    public function KartSprite(kartType :String, ground :Ground = null, stopFrame :int = 1)
    {
        try {
            _kart = new KartSprite[kartType]();
            _movementConstants = KartSprite[kartType + "_Movement"];
        } catch (re :ReferenceError) {
            throw new ArgumentError(kartType + " is not a recognized Kart Type");
        }
        if (stopFrame != -1) {
            _kart.gotoAndStop(stopFrame);
        }
        addChild(_kart);
        _kartType = kartType;
        _ground = ground;
    }

    public function get kartType () :String
    {
        return _kartType;
    }

    public function shieldsUp (up :Boolean) :void 
    {
        if (up) {
            if (_shield == null) {
                _shield = Bonus.getGameSprite(Bonus.SHIELD);
                _shield.y -= _shield.height / 2;
                addChild(_shield);
            }
        } else {
            if (_shield != null) {
                removeChild(_shield);
                _shield = null;
            }
        }
    }

    /**
     * returns the amount of time that has passed since the last time this function was called
     */
    protected function timeSinceUpdate () :Number
    {
        var time :Number = 0;
        var now :uint = getTimer();
        if (_lastUpdate != 0) {
            time = (now - _lastUpdate) / 1000;
        } 
        _lastUpdate = now;
        return time;
    }

    /** 
     * 

    /**
     * This is really only used by the subclasses, but its needed by both.
     */
    protected function calculateNewPosition (time :Number) :void
    {
        // update current facing angle
        if (_movement & (MOVEMENT_RIGHT | MOVEMENT_LEFT)) {
            if (_movement & MOVEMENT_RIGHT) {
                _currentTurnAngle = Math.min(MAX_TURN_ANGLE, _currentTurnAngle + 
                    TURN_ACCELERATION * time);
            } else {
                _currentTurnAngle = Math.max(-MAX_TURN_ANGLE, _currentTurnAngle - 
                    TURN_ACCELERATION * time);
            }
        } else {
            if (_currentTurnAngle > 0) {
                _currentTurnAngle = Math.max(0, _currentTurnAngle - TURN_ACCELERATION * time);
            } else if (_currentTurnAngle < 0) {
                _currentTurnAngle = Math.min(0, _currentTurnAngle + TURN_ACCELERATION * time);
            }
        }
        if (_currentSpeed > 0) {
            _currentAngle = (_currentAngle + _currentTurnAngle) % (Math.PI * 2);
        } else if (_currentSpeed < 0) {
            _currentAngle = (_currentAngle - _currentTurnAngle + Math.PI * 2) % (Math.PI * 2);
        }

        // calculate the new speed
        var maxSpeed :Number = _movementConstants.maxSpeed;
        var minSpeed :Number = _movementConstants.minSpeed;
        var accelGas :Number = _movementConstants.accelGas;
        var accelBrake :Number = _movementConstants.accelBrake;
        var accelCoast :Number = _movementConstants.accelCoast;
        if (!_ground.getLevel().isOnRoad(_currentPosition)) {
            // terrain doesn't affect min speed
            maxSpeed *= TERRAIN_SPEED_FACTOR;
            accelGas *= TERRAIN_SPEED_FACTOR;
            accelBrake /= TERRAIN_SPEED_FACTOR;
            accelCoast /= TERRAIN_SPEED_FACTOR;
        }

        if (_movement & MOVEMENT_FORWARD) {
            if (_currentSpeed >= 0) {
                // if we're going faster than the max, then we probably just went off road...
                // force a slow-down, but do it gently
                if (_currentSpeed > maxSpeed) {
                    _currentSpeed = Math.max(maxSpeed, _currentSpeed - accelCoast * time);
                } else {
                    _currentSpeed = Math.min(maxSpeed, _currentSpeed + accelGas * time);
                }
            } else { 
                _currentSpeed += accelBrake * time;
            }
        } else if (_movement & MOVEMENT_BACKWARD) {
            if (_currentSpeed > 0) {
                _currentSpeed = Math.max(0, _currentSpeed - accelBrake * time);
            } else {
               _currentSpeed = Math.max(minSpeed, _currentSpeed - accelGas * time);
            }
        } else {
            if (_currentSpeed > 0) {
                _currentSpeed = Math.max(0, _currentSpeed - accelCoast * time);
            } else {
                _currentSpeed = Math.min(0, _currentSpeed + accelCoast * time);
            }
        }

        // calculate the new position, based on the new speed and new angle
        var newPosition :Point;
        var rotation :Matrix = new Matrix();
        rotation.rotate(_currentAngle);
        if ((_movement & MOVEMENT_DRIFT) && _jumpFrameCount == 0) { 
            var driftSpeed :Number = _currentSpeed * DRIFT_Y_SPEED_FACTOR;
            if (_movement & MOVEMENT_RIGHT) {
                driftSpeed *= -1;
            }
            newPosition = _currentPosition.add(rotation.transformPoint(new Point(
                driftSpeed * time, 0)));
            driftSpeed = _currentSpeed * DRIFT_X_SPEED_FACTOR;
            newPosition = newPosition.add(rotation.transformPoint(new Point(
                0, -driftSpeed * time)));
        } else {
            newPosition = _currentPosition.add(rotation.transformPoint(new Point(0, 
                -_currentSpeed * time)));
        }

        var collides :Object = _ground.getScenery().getCollidingObject();
        if ((collides != null && (collides.sceneryType == Scenery.OBSTACLE || 
            collides.sceneryType == Scenery.KART)) || _ground.getLevel().isOnWall(newPosition)) {
            newPosition = bounce(_currentPosition, newPosition);
        } else if (collides != null && collides.sceneryType == Scenery.FIREBALL) {
            if (this is Kart && !collides.isMyFireball) {
                _ground.getScenery().removeFireball(collides);
                if (_shield == null) {
                    (this as Kart).killMovement();
                } else {
                    dispatchEvent(new KartEvent(KartEvent.SHIELD, false));
                }
            } else if (!(this is Kart)) {
                // dunno why this isn't working
                //_ground.getScenery().removeFireball(collides);
            }
        }
        _currentPosition = newPosition;
    }

    /**
     * Bounce off of the new point, by interpolating past the old point.  This should be improved 
     * so that it take into account the direction you hit it from, and bounces you off at 
     * the correct angle.  Also, in the case of karts, the larger karts have more influence that
     * smaller karts... they should be bounced less.
     */
    protected function bounce (oldPos :Point, newPos :Point) :Point 
    {
        _currentSpeed *= 0.75;
        return Point.interpolate(oldPos, newPos, 1.5);
    }

    /** light kart swf */
    [Embed(source='rsrc/lightkart.swf#kart')]
    protected static const LightKart :Class;
    protected static const LightKart_Movement :Object = {
        maxSpeed: 12 * SPEED_FACTOR,
        minSpeed: -0.5 * SPEED_FACTOR, 
        accelGas: 0.4 * SPEED_FACTOR * SPEED_FACTOR,
        accelBrake: 1 * SPEED_FACTOR * SPEED_FACTOR,
        accelCoast: 0.25 * SPEED_FACTOR * SPEED_FACTOR
    };

    /** medium kart swf */
    [Embed(source='rsrc/mediumkart.swf#kart')]
    protected static const MediumKart :Class;
    protected static const MediumKart_Movement :Object = {
        maxSpeed: 13 * SPEED_FACTOR,
        minSpeed: -0.5 * SPEED_FACTOR,
        accelGas: 0.3 * SPEED_FACTOR * SPEED_FACTOR,
        accelBrake: 0.8 * SPEED_FACTOR * SPEED_FACTOR,
        accelCoast: 0.3 * SPEED_FACTOR * SPEED_FACTOR
    };

    /** heavy kart swf */
    [Embed(source='rsrc/heavykart.swf#kart')]
    protected static const HeavyKart :Class;
    protected static const HeavyKart_Movement :Object = {
        maxSpeed: 15 * SPEED_FACTOR,
        minSpeed: -0.5 * SPEED_FACTOR,
        accelGas: 0.2 * SPEED_FACTOR * SPEED_FACTOR,
        accelBrake: 0.6 * SPEED_FACTOR * SPEED_FACTOR,
        accelCoast: 0.35 * SPEED_FACTOR * SPEED_FACTOR
    };

    /** flags for the _movement bit flag variable */
    protected static const MOVEMENT_FORWARD :int = 0x01;
    protected static const MOVEMENT_BACKWARD :int = 0x02;
    protected static const MOVEMENT_LEFT :int = 0x04;
    protected static const MOVEMENT_RIGHT :int = 0x08;
    protected static const MOVEMENT_DRIFT :int = 0x10;

    /** constants to control kart motion properties */
    protected static const MAX_TURN_ANGLE :Number = 0.0436; // 2.5 degrees 0.0524; // 3 degrees
    protected static const TURN_ACCELERATION :Number = 0.008 * SPEED_FACTOR;

    /** values to control drifting */
    protected static const DRIFT_X_SPEED_FACTOR :Number = 0.5;
    protected static const DRIFT_Y_SPEED_FACTOR :Number = 0.5;

    /** Factor to cut speed by when driving off-road */
    protected static const TERRAIN_SPEED_FACTOR :Number = 0.2;

    /** The amount to multiply our speed values by to bring them into the time domain */
    protected static const SPEED_FACTOR :int = 20;

    protected var _kart :MovieClipAsset;
     
    protected var _kartType :String;

    protected var _currentSpeed :Number = 0;
    protected var _currentAngle :Number;
    protected var _currentPosition: Point;

    /** The current amount we are adding or subtracting from the kart's turn angle */
    protected var _currentTurnAngle :Number = 0;

    /** Bit flags to indicate which movement keys are pressed */
    protected var _movement :int = 0;

    /** Frames left before the jump is over */
    protected var _jumpFrameCount :int = 0;

    protected var _movementConstants :Object;

    /** reference to the level object */
    protected var _ground :Ground;

    protected var _shield :Sprite;

    protected var _lastUpdate :uint = 0;
}
}
