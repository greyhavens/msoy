package {
import flash.display.Sprite;

import flash.geom.Matrix;
import flash.geom.Point;

import flash.events.Event;

import flash.utils.getTimer;

import mx.core.MovieClipAsset;

import com.threerings.util.Line;

public class Kart extends KartSprite
{
    public function Kart (kartType :String, camera :Camera, ground :Ground) 
    {
        super(kartType, ground);
        _camera = camera;
        ground.getScenery().registerKart(this);

        addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    /**
     * Get an update object to send to the other clients.
     */
    public function getUpdate () :Object
    {
        var loc :Point = _ground.getKartLocation();
        return {
            posX: loc.x,
            posY: loc.y,
            angle: _camera.angle,
            speed: _currentSpeed,
            movement: _movement
        };
    }

    public function moveForward (moving :Boolean) :void 
    { 
        keyAction(moving, MOVEMENT_FORWARD);
    }

    public function moveBackward (moving :Boolean) :void
    {
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
    
    public function killMovement () :void
    {
        _currentSpeed = 0;
        _movement = 0;
        _currentTurnAngle = 0;
        _currentViewAngle = 0;
    }

    public function activateBonus () :void
    {
        if (_bonus != null) {
            dispatchEvent(new KartEvent(KartEvent.REMOVE_BONUS, _bonus));
            _bonus.activate(this);
            _bonus = null;
        }
    }

    public function destroyBonus () :void
    {
        dispatchEvent(new KartEvent(KartEvent.REMOVE_BONUS, _bonus));
        _bonus = null;
    }

    public function boostSpeed (percent :Number = 1) :void 
    {
        _currentSpeed = Math.min(_movementConstants.maxSpeed * 2, _currentSpeed + percent * BOOST);
    }

    override public function shieldsUp (up :Boolean) :void
    {
        var shield :Sprite = _shield;
        super.shieldsUp(up);
        if (up && shield != _shield) {
            _shield.addEventListener(Event.ENTER_FRAME, function (startTime :int) :Function {
                var frameListener :Function = function (evt :Event) :void {
                    if (getTimer() - startTime > Bonus.SHIELD_DURATION) {
                        evt.target.removeEventListener(Event.ENTER_FRAME, frameListener);
                        // this shield may have been removed previous to this expiration
                        if (evt.target == _shield) {
                            dispatchEvent(new KartEvent(KartEvent.SHIELD, false));
                        }
                    }
                }
                return frameListener;
            }(getTimer()));
        }
    }

    protected function enterFrame (event :Event) :void
    {
        // update camera and kart angles
        var viewAcceleration :int = Math.abs(_currentViewAngle) > TURN_VIEW_ANGLE ? 
            VIEW_ACCELERATION * 3 : VIEW_ACCELERATION;
        if (_movement & (MOVEMENT_RIGHT | MOVEMENT_LEFT)) {
            var maxViewAngle :int = _movement & MOVEMENT_DRIFT ? DRIFT_VIEW_ANGLE : 
                TURN_VIEW_ANGLE;
            if (_movement & MOVEMENT_RIGHT) {
                _currentTurnAngle = Math.min(MAX_TURN_ANGLE, _currentTurnAngle + TURN_ACCELERATION);
                _currentViewAngle = Math.min(maxViewAngle, _currentViewAngle + viewAcceleration);
            } else {
                _currentTurnAngle = Math.max(-MAX_TURN_ANGLE, _currentTurnAngle - 
                    TURN_ACCELERATION);
                _currentViewAngle = Math.max(-maxViewAngle, _currentViewAngle - viewAcceleration);
            }
        } else {
            if (_currentTurnAngle > 0) {
                _currentTurnAngle = Math.max(0, _currentTurnAngle - TURN_ACCELERATION);
            } else if (_currentTurnAngle < 0) {
                _currentTurnAngle = Math.min(0, _currentTurnAngle + TURN_ACCELERATION);
            }
            if (_currentViewAngle > 0) {
                _currentViewAngle = Math.max(0, _currentViewAngle - viewAcceleration);
            } else if (_currentViewAngle < 0) {
                _currentViewAngle = Math.min(0, _currentViewAngle + viewAcceleration);
            }
        }
        if (_currentSpeed > 0) {
            _camera.angle = (_camera.angle + _currentTurnAngle) % (Math.PI * 2);
        } else if (_currentSpeed < 0) {
            _camera.angle = (_camera.angle - _currentTurnAngle + Math.PI * 2) % (Math.PI * 2);
        }

        // update turn animation
        var frame :int = Math.abs(_currentViewAngle) as int;
        if (_currentViewAngle > 0) {
            frame = 360 - frame;
        } else if (_currentViewAngle == 0) {
            frame = 1;
        }
        if (_kart.currentFrame != frame) {
            _kart.gotoAndStop(frame);
        }

        var oldPos :Point = _camera.position;
        _camera.position = calculateNewPosition(_camera.position, _camera.angle,    
            _ground.getKartLocation());
        var movedLine :Line = new Line(oldPos, _camera.position);
        var intersection :int = _ground.getLevel().getFinishLine().getIntersectionType(movedLine);
        if (intersection == Line.INTERSECTION_NORTH) {
            dispatchEvent(new KartEvent(KartEvent.CROSSED_FINISH_LINE, 1));
        } else if (intersection == Line.INTERSECTION_SOUTH) {
            dispatchEvent(new KartEvent(KartEvent.CROSSED_FINISH_LINE, -1));
        }

        var boosts :Array = _ground.getLevel().getBoosts();
        for each (var boost :Object in _ground.getLevel().getBoosts()) {
            if (boost.line.isIntersected(movedLine)) {
                var angle :Number = (Math.PI / 2) * 
                    (boost.type - LevelConfig.OBJECT_BOOST_POINT_EAST);
                angle = angle == 0 ? Math.PI * 2 : angle;
                var percent :Number = 1 - (Math.abs(-_camera.angle + Math.PI / 2 - angle) % 
                    (Math.PI * 2)) / (Math.PI / 2);
                // TODO: figure out what's going on, and do this the right way
                percent = percent > 1 ? 0 : (percent < 0 ? 1 : percent);
                boostSpeed(percent);
            } 
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

        var collides :Object = _ground.getScenery().getCollidingObject();
        if (collides != null && collides.sceneryType == Scenery.BONUS) {
            _ground.getScenery().removeObject(collides);
            if (_bonus == null) {
                dispatchEvent(new KartEvent(KartEvent.BONUS, _bonus = new Bonus()));
            }
        }
    }

    protected function keyAction (inMotion :Boolean, flag :int) :void
    {
        if (inMotion) {
            _movement |= flag;
        } else {
            _movement &= ~flag;
        }
    }

    /** turning constants */
    protected static const TURN_VIEW_ANGLE :int = 15; // in degrees
    protected static const DRIFT_VIEW_ANGLE :int = 45; // in degrees
    protected static const VIEW_ACCELERATION :int = 4; // degrees per frame

    protected static const BOOST :Number = 5;

    /** values to control jumping */
    protected static const JUMP_DURATION :int = 3;
    protected static const JUMP_HEIGHT :int = 15;
   
    /** reference to the camera object */
    protected var _camera :Camera;

    /** The current amount we are adding or subtracting from the kart's turn angle */
    protected var _currentTurnAngle :Number = 0;

    /** The current angle we are viewing our own kart at */
    protected var _currentViewAngle :Number = 0;

    protected var _bonus :Bonus;
}
}
