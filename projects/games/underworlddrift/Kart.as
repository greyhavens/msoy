package {
import flash.display.Sprite;

import flash.geom.Matrix;
import flash.geom.Point;

import flash.events.Event;

public class Kart extends Sprite
{
    public function Kart (camera :Camera)
    {
        _camera = camera;
        _currentKart = new BOWSER_CENTERED();
        addChild(_currentKart);

        addEventListener(Event.ENTER_FRAME, enterFrame);
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
    }

    public function turnRight (turning :Boolean) :void
    {
        keyAction(turning, MOVEMENT_RIGHT);
    }

    public function enterFrame (event :Event) :void
    {
        // TODO: base these speeds on something fairer than enterFrame.  Using this method,
        // the person with the fastest computer (higher framerate) gets to drive more quickly.
        // rotate camera
        if (_movement & MOVEMENT_RIGHT) {
            _camera.angle += 0.0745;
        } else if (_movement & MOVEMENT_LEFT) {
            _camera.angle -= 0.0745;
        }

        var rotation :Matrix;
        if (_movement & MOVEMENT_FORWARD) {
            rotation = new Matrix();
            rotation.rotate(_camera.angle);
            _camera.position = _camera.position.add(rotation.transformPoint(new Point(0, -10)));
        } else if (_movement & MOVEMENT_BACKWARD) {
            rotation = new Matrix();
            rotation.rotate(_camera.angle);
            _camera.position = _camera.position.add(rotation.transformPoint(new Point(0, 10)));
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

    /** Bit flags to indicate which movement keys are pressed */
    protected var _movement :int = 0;

    /** reference to ground object */
    protected var _camera :Camera;

    /** Embedded cart sprite */
    protected var _currentKart :Sprite;

    /** Kart's current speed */
    protected var _currentSpeed :Sprite;

    /** Bowser Kart TODO: switch swf to a single movie, with all the frames embedded */
    [Embed(source='rsrc/bowser.swf#bowser_centered')]
    protected static const BOWSER_CENTERED :Class;
    [Embed(source='rsrc/bowser.swf#bowser_left')]
    protected static const BOWSER_LEFT_TURN :Class;
    [Embed(source='rsrc/bowser.swf#bowser_right')]
    protected static const BOWSER_RIGHT_TURN :Class;

    protected static const SPEED_MAX :int = 20;
    protected static const SPEED_MIN :int = -5;
    protected static const ACCELERATION :Number = 0.5;

    protected static const MOVEMENT_FORWARD :int = 0x01;
    protected static const MOVEMENT_BACKWARD :int = 0x02;
    protected static const MOVEMENT_LEFT :int = 0x04;
    protected static const MOVEMENT_RIGHT :int = 0x08;
}
}
