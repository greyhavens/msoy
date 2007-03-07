package {

import flash.display.Sprite;

import flash.geom.Point;
import flash.geom.Matrix;

import mx.core.MovieClipAsset;

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

    /**
     * This is really only used by the subclasses, but its needed by both.
     */
    protected function calculateNewPosition (position :Point, cameraAngle :Number, 
        kartLocation :Point) :Point
    {
        // calculate the new speed
        var maxSpeed :Number = _movementConstants.maxSpeed;
        var minSpeed :Number = _movementConstants.minSpeed;
        var accelGas :Number = _movementConstants.accelGas;
        var accelBrake :Number = _movementConstants.accelBrake;
        var accelCoast :Number = _movementConstants.accelCoast;
        if (!_ground.getLevel().isOnRoad(kartLocation)) {
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
                    _currentSpeed = Math.max(maxSpeed, _currentSpeed - accelCoast);
                } else {
                    _currentSpeed = Math.min(maxSpeed, _currentSpeed + accelGas);
                }
            } else { 
                _currentSpeed += accelBrake;
            }
        } else if (_movement & MOVEMENT_BACKWARD) {
            if (_currentSpeed > 0) {
                _currentSpeed = Math.max(0, _currentSpeed - accelBrake);
            } else {
               _currentSpeed = Math.max(minSpeed, _currentSpeed - accelGas);
            }
        } else {
            if (_currentSpeed > 0) {
                _currentSpeed = Math.max(0, _currentSpeed - accelCoast);
            } else {
                _currentSpeed = Math.min(0, _currentSpeed + accelCoast);
            }
        }

        // calculate the new position, based on the new speed
        var newPosition :Point;
        var rotation :Matrix = new Matrix();
        rotation.rotate(cameraAngle);
        if ((_movement & MOVEMENT_DRIFT) && _jumpFrameCount == 0) { 
            var driftSpeed :Number = _currentSpeed * DRIFT_Y_SPEED_FACTOR;
            if (_movement & MOVEMENT_RIGHT) {
                driftSpeed *= -1;
            }
            newPosition = position.add(rotation.transformPoint(new Point(
                driftSpeed, 0)));
            driftSpeed = _currentSpeed * DRIFT_X_SPEED_FACTOR;
            newPosition = newPosition.add(rotation.transformPoint(new Point(
                0, -driftSpeed)));
        } else {
            newPosition = position.add(rotation.transformPoint(new Point(0, 
                -_currentSpeed)));
        }

        var collides :Object = _ground.getScenery().getCollidingObject();
        if ((collides != null && (collides.sceneryType == Scenery.OBSTACLE || 
            collides.sceneryType == Scenery.KART)) || _ground.getLevel().isOnWall(newPosition)) {
            return bounce(position, newPosition);
        } else {
            return newPosition;
        }
    }

    /**
     * Bounce off of the new point, by interpolating past the old point.  This should be improved 
     * so that it take into account the direction you hit it from, and bounces you off at 
     * the correct angle.  Also, in the case of karts, the larger karts have more influence that
     * smaller karts... they are bounced less.
     */
    protected function bounce (oldPos :Point, newPos :Point) :Point 
    {
        return Point.interpolate(oldPos, newPos, 3);
    }

    /** light kart swf */
    [Embed(source='rsrc/lightkart.swf#kart')]
    protected static const LightKart :Class;
    protected static const LightKart_Movement :Object = {
        maxSpeed: 12,
        minSpeed: -0.5, 
        accelGas: 0.4,
        accelBrake: 1,
        accelCoast: 0.25
    };

    /** medium kart swf */
    [Embed(source='rsrc/mediumkart.swf#kart')]
    protected static const MediumKart :Class;
    protected static const MediumKart_Movement :Object = {
        maxSpeed: 13,
        minSpeed: -0.5,
        accelGas: 0.3,
        accelBrake: 0.8,
        accelCoast: 0.3
    };

    /** heavy kart swf */
    [Embed(source='rsrc/heavykart.swf#kart')]
    protected static const HeavyKart :Class;
    protected static const HeavyKart_Movement :Object = {
        maxSpeed: 15,
        minSpeed: -0.5,
        accelGas: 0.2,
        accelBrake: 0.6,
        accelCoast: 0.35
    };

    /** flags for the _movement bit flag variable */
    protected static const MOVEMENT_FORWARD :int = 0x01;
    protected static const MOVEMENT_BACKWARD :int = 0x02;
    protected static const MOVEMENT_LEFT :int = 0x04;
    protected static const MOVEMENT_RIGHT :int = 0x08;
    protected static const MOVEMENT_DRIFT :int = 0x10;

    /** constants to control kart motion properties */
    protected static const MAX_TURN_ANGLE :Number = 0.0436; // 2.5 degrees 0.0524; // 3 degrees
    protected static const TURN_ACCELERATION :Number = 0.008;

    /** values to control drifting */
    protected static const DRIFT_X_SPEED_FACTOR :Number = 0.5;
    protected static const DRIFT_Y_SPEED_FACTOR :Number = 0.5;

    /** Factor to cut speed by when driving off-road */
    protected static const TERRAIN_SPEED_FACTOR :Number = 0.2;

    protected var _kart :MovieClipAsset;
     
    protected var _kartType :String;

    protected var _currentSpeed :Number = 0;

    /** Bit flags to indicate which movement keys are pressed */
    protected var _movement :int = 0;

    /** Frames left before the jump is over */
    protected var _jumpFrameCount :int = 0;

    protected var _movementConstants :Object;

    /** reference to the level object */
    protected var _ground :Ground;
}
}
