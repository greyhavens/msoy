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

    public function KartSprite(kartType :String, stopFrame :int = 1)
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
    }

    public function get kartType () :String
    {
        return _kartType;
    }

    /**
     * This is really only used by the subclasses, but its needed by both.
     */
    protected function calculateNewPosition (position :Point, cameraAngle :Number) :Point
    {
        var rotation :Matrix;
        if (_movement & MOVEMENT_FORWARD) {
            if (_currentSpeed >= 0) {
                _currentSpeed = Math.min(_movementConstants.maxSpeed, 
                    _currentSpeed + _movementConstants.accelGas);
            } else { 
                _currentSpeed += _movementConstants.accelBrake;
            }
        } else if (_movement & MOVEMENT_BACKWARD) {
            if (_currentSpeed <= 0 && !_braking) {
                _currentSpeed = Math.max(_movementConstants.minSpeed, 
                    _currentSpeed - _movementConstants.accelGas);
            } else {
                _currentSpeed = Math.max(0, _currentSpeed - _movementConstants.accelGas);
            }
        } else {
            if ((_currentSpeed > _movementConstants.accelCoast && _currentSpeed > 0) || 
                (_currentSpeed < _movementConstants.accelCoast && _currentSpeed < 0)) {
                if (_currentSpeed > 0) {
                    _currentSpeed -= _movementConstants.accelCoast;
                } else {
                    _currentSpeed += _movementConstants.accelCoast;
                }
            } else {
                _currentSpeed = 0;
            }
        }
        var newPosition :Point;
        rotation = new Matrix();
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
        return newPosition;
    }

    /** light kart swf */
    [Embed(source='rsrc/lightkart.swf#kart')]
    protected static const LightKart :Class;
    protected static const LightKart_Movement :Object = {
        maxSpeed: 12,
        minSpeed: 0.5, 
        accelGas: 0.4,
        accelBrake: 2.5,
        accelCoast: 0.35
    };

    /** medium kart swf */
    [Embed(source='rsrc/mediumkart.swf#kart')]
    protected static const MediumKart :Class;
    protected static const MediumKart_Movement :Object = {
        maxSpeed: 13,
        minSpeed: 0.5,
        accelGas: 0.3,
        accelBrake: 2,
        accelCoast: 0.3
    };

    /** heavy kart swf */
    [Embed(source='rsrc/heavykart.swf#kart')]
    protected static const HeavyKart :Class;
    protected static const HeavyKart_Movement :Object = {
        maxSpeed: 15,
        minSpeed: 0.5,
        accelGas: 0.2,
        accelBrake: 1.5,
        accelCoast: 0.25
    };

    /** flags for the _movement bit flag variable */
    protected static const MOVEMENT_FORWARD :int = 0x01;
    protected static const MOVEMENT_BACKWARD :int = 0x02;
    protected static const MOVEMENT_LEFT :int = 0x04;
    protected static const MOVEMENT_RIGHT :int = 0x08;
    protected static const MOVEMENT_DRIFT :int = 0x10;

    /** constants to control kart motion properties */
    protected static const MAX_TURN_ANGLE :Number = 0.0524; // 3 degrees
    protected static const TURN_ACCELERATION :Number = 0.015;

    /** values to control drifting */
    protected static const DRIFT_X_SPEED_FACTOR :Number = 0.2;
    protected static const DRIFT_Y_SPEED_FACTOR :Number = 0.5;

    protected var _kart :MovieClipAsset;
     
    protected var _kartType :String;

    /** a user must lift their finger and re-apply in order to go backwards after braking */
    protected var _braking :Boolean = false;

    protected var _currentSpeed :Number = 0;

    /** Bit flags to indicate which movement keys are pressed */
    protected var _movement :int = 0;

    /** Frames left before the jump is over */
    protected var _jumpFrameCount :int = 0;

    protected var _movementConstants :Object;
}
}
