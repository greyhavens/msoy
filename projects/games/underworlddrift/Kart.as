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
        _movingForward = moving;
    }

    public function moveBackward (moving :Boolean) :void
    {
        _movingBackward = moving;
    }

    public function turnLeft (turning :Boolean) :void
    {
        _turningLeft = turning;
    }

    public function turnRight (turning :Boolean) :void
    {
        _turningRight = turning
    }

    public function enterFrame (event :Event) :void
    {
        // TODO: base these speeds on something fairer than enterFrame.  Using this method,
        // the person with the fastest computer (higher framerate) gets to drive more quickly.
        // rotate camera
        if (_turningRight) {
            _camera.angle += 0.0745;
        } else if (_turningLeft) {
            _camera.angle -= 0.0745;
        }

        // move camera TODO: do something better than dual booleans
        var rotation :Matrix;
        if (_movingForward || _movingBackward) {
            if (_movingForward) {
                rotation = new Matrix();
                rotation.rotate(_camera.angle);
                _camera.position = _camera.position.add(rotation.transformPoint(new Point(0, -10)));
            } else if (_movingBackward) {
                rotation = new Matrix();
                rotation.rotate(_camera.angle);
                _camera.position = _camera.position.add(rotation.transformPoint(new Point(0, 10)));
            }
         }
    }

    /** flag to indicate forward movement */
    protected var _movingForward :Boolean = false;

    /** flag to indicate backward movement */
    protected var _movingBackward :Boolean = false;

    /** flag to indicate rotation to the right */
    protected var _turningRight :Boolean = false;

    /** flag to indicate rotation to the left */
    protected var _turningLeft :Boolean = false;

    /** reference to ground object */
    protected var _camera :Camera;

    /** Embedded cart sprite */
    protected var _currentKart :Sprite;

    /** Bowser Kart TODO: switch swf to a single movie, with all the frames embedded */
    [Embed(source='rsrc/bowser.swf#bowser_centered')]
    protected static const BOWSER_CENTERED :Class;
    [Embed(source='rsrc/bowser.swf#bowser_left')]
    protected static const BOWSER_LEFT_TURN :Class;
    [Embed(source='rsrc/bowser.swf#bowser_right')]
    protected static const BOWSER_RIGHT_TURN :Class;
}
}
