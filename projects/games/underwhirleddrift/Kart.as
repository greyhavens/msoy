package {
import flash.display.Sprite;

import flash.geom.Matrix;
import flash.geom.Point;

import flash.events.Event;

import flash.utils.describeType;

import mx.core.MovieClipAsset;

public class Kart extends Sprite
{
    public function Kart (camera :Camera, ground :Ground)
    {
        _camera = camera;
        _ground = ground;
        _kart = new KART();
        addChild(_kart);

        addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    public function moveForward (moving :Boolean) :void 
    { 
        keyAction(moving, MOVEMENT_FORWARD);
    }

    public function moveBackward (moving :Boolean) :void
    {
        if (moving && _currentSpeed > 0) {
            _braking = true;
        } else if (!moving && _braking) {
            _braking = false;
        }
        keyAction(moving, MOVEMENT_BACKWARD);
    }

    public function turnLeft (turning :Boolean) :void
    {
        keyAction(turning, MOVEMENT_LEFT);
        if (!turning) keyAction(false, MOVEMENT_DRIFT);
    }

    public function turnRight (turning :Boolean) :void
    {
        keyAction(turning, MOVEMENT_RIGHT);
        if (!turning) keyAction(false, MOVEMENT_DRIFT);
    }

    public function jump () :void
    {
        if (_jumpFrameCount == 0) {
            _jumpFrameCount = JUMP_DURATION;
            y -= JUMP_HEIGHT;
        }
    }

    public function enterFrame (event :Event) :void
    {
        // TODO: base these speeds on something fairer than enterFrame.  Using this method,
        // the person with the fastest computer (higher framerate) gets to drive more quickly.
        // rotate camera

        // alter camera angle
        if (_movement & (MOVEMENT_RIGHT | MOVEMENT_LEFT)) {
            if (_movement & MOVEMENT_RIGHT) {
                _currentAngle = Math.min(MAX_TURN_ANGLE, _currentAngle + TURN_ACCELERATION);
            } else {
                _currentAngle = Math.max(-MAX_TURN_ANGLE, _currentAngle - TURN_ACCELERATION);
            }
        } else {
            if (_currentAngle > 0) {
                _currentAngle = Math.max (0, _currentAngle - TURN_ACCELERATION);
            } else if (_currentAngle < 0) {
                _currentAngle = Math.min (0, _currentAngle + TURN_ACCELERATION);
            }
        }
        if (_currentSpeed > 0) {
            _camera.angle += _currentAngle;
        } else if (_currentSpeed < 0) {
            _camera.angle -= _currentAngle;
        }
        // update turn animation
        var max_view_angle :int = _movement & MOVEMENT_DRIFT ? DRIFT_VIEW_ANGLE : TURN_VIEW_ANGLE;
        var frame :int = Math.ceil((Math.abs(_currentAngle) / MAX_TURN_ANGLE) * max_view_angle);
        if (_currentAngle > 0) {
            frame = 360 - frame;
        } else if (_currentAngle == 0) {
            frame = 1;
        }
        if (_kart.currentFrame != frame) {
            _kart.gotoAndStop(frame);
        }

        // alter camera location
        var gasAccel :Number = ACCELERATION_GAS;
        var coastAccel :Number = ACCELERATION_COAST;
        var brakeAccel :Number = ACCELERATION_BRAKE;
        var minSpeed :Number = SPEED_MIN;
        var maxSpeed :Number = SPEED_MAX;
        if (!_ground.drivingOnRoad()) {
            gasAccel *= TERRAIN_SPEED_FACTOR;
            coastAccel /= TERRAIN_SPEED_FACTOR;
            brakeAccel /= TERRAIN_SPEED_FACTOR;
            minSpeed *= TERRAIN_SPEED_FACTOR;
            maxSpeed *= TERRAIN_SPEED_FACTOR;
        }
        // TODO: this will clearly need more intelligent processing
        //if (_ground.drivingIntoWall()) {
            //_currentSpeed = 0;
        //}
        var rotation :Matrix;
        if (_movement & MOVEMENT_FORWARD) {
            if (_currentSpeed >= 0) {
                _currentSpeed = Math.min(maxSpeed, _currentSpeed + gasAccel);
            } else { 
                _currentSpeed += brakeAccel;
            }
        } else if (_movement & MOVEMENT_BACKWARD) {
            if (_currentSpeed <= 0 && !_braking) {
                _currentSpeed = Math.max(minSpeed, _currentSpeed - gasAccel);
            } else {
                _currentSpeed = Math.max(0, _currentSpeed - gasAccel);
            }
        } else {
            if ((_currentSpeed > coastAccel && _currentSpeed > 0) || 
                (_currentSpeed < coastAccel && _currentSpeed < 0)) {
                if (_currentSpeed > 0) {
                    _currentSpeed -= coastAccel;
                } else {
                    _currentSpeed += coastAccel;
                }
            } else {
                _currentSpeed = 0;
            }
        }
        rotation = new Matrix();
        rotation.rotate(_camera.angle);
        if ((_movement & MOVEMENT_DRIFT) && _jumpFrameCount == 0) { 
            var driftSpeed :Number = _currentSpeed * DRIFT_Y_SPEED_FACTOR;
            if (_movement & MOVEMENT_RIGHT) {
                driftSpeed *= -1;
            }
            _camera.position = _camera.position.add(rotation.transformPoint(new Point(
                driftSpeed, 0)));
            driftSpeed = _currentSpeed * DRIFT_X_SPEED_FACTOR;
            _camera.position = _camera.position.add(rotation.transformPoint(new Point(
                0, -driftSpeed)));
        } else {
            _camera.position = _camera.position.add(rotation.transformPoint(new Point(0, 
                -_currentSpeed)));
        }

        // deal with a jump
        if (_jumpFrameCount > 0) {
            _jumpFrameCount--;
            if (_jumpFrameCount == 0) {
                y += JUMP_HEIGHT;
                if (_movement & (MOVEMENT_RIGHT | MOVEMENT_LEFT)) {
                    keyAction(true, MOVEMENT_DRIFT);
                }
            }
        }
    }

    protected function keyAction (inMotion :Boolean, flag :int) :void
    {
        if (inMotion) {
            _movement |= flag;
        } else {
            // if we're turning off drifting and it was on), cut back _currentSpeed
            if (flag == MOVEMENT_DRIFT && (_movement & MOVEMENT_DRIFT)) {
                _currentSpeed *= (DRIFT_X_SPEED_FACTOR + DRIFT_Y_SPEED_FACTOR) / 2;
            }
            _movement &= ~flag;
        }
    }

    /** Bit flags to indicate which movement keys are pressed */
    protected var _movement :int = 0;
    
    /** a user must lift their finger and re-apply in order to go backwards after braking */
    protected var _braking :Boolean = false;

    /** reference to the camera object */
    protected var _camera :Camera;

    /** reference to the ground object */
    protected var _ground :Ground;

    /** Embedded cart movie clip */
    protected var _kart :MovieClipAsset;

    /** Kart's current speed */
    protected var _currentSpeed :Number = 0;

    /** Kart's current turn angle */
    protected var _currentAngle :Number = 0;

    /** Frames left before the jump is over */
    protected var _jumpFrameCount :int = 0;

    /** Kart swf */
    [Embed(source='rsrc/mediumkart.swf#kart')]
    protected static const KART :Class;

    /** turning constants */
    protected static const TURN_VIEW_ANGLE :int = 15; // in degrees
    protected static const DRIFT_VIEW_ANGLE :int = 45; // in degrees

    /** constants to control kart motion properties */
    protected static const SPEED_MAX :int = 15; // 25;
    protected static const SPEED_MIN :int = -5;
    protected static const ACCELERATION_GAS :Number = 0.3; //0.5;
    protected static const ACCELERATION_BRAKE :Number = 2;
    protected static const ACCELERATION_COAST :Number = 0.2; //0.5;
    protected static const MAX_TURN_ANGLE :Number = 0.0524; // 3 degrees
    protected static const TURN_ACCELERATION :Number = 0.015;

    /** flags for the _movement bit flag variable */
    protected static const MOVEMENT_FORWARD :int = 0x01;
    protected static const MOVEMENT_BACKWARD :int = 0x02;
    protected static const MOVEMENT_LEFT :int = 0x04;
    protected static const MOVEMENT_RIGHT :int = 0x08;
    protected static const MOVEMENT_DRIFT :int = 0x10;

    /** values to control jumping */
    protected static const JUMP_DURATION :int = 3;
    protected static const JUMP_HEIGHT :int = 15;

    /** values to control drifting */
    protected static const DRIFT_X_SPEED_FACTOR :Number = 0.2;
    protected static const DRIFT_Y_SPEED_FACTOR :Number = 0.5;

    /** Factor to cut speed by when driving off-road */
    protected static const TERRAIN_SPEED_FACTOR :Number = 0.2;
}
}
