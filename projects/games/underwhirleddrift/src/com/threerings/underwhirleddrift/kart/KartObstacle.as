package com.threerings.underwhirleddrift.kart {

import flash.display.Sprite;

import flash.geom.Point;
import flash.geom.Matrix;

import flash.events.Event;

import com.threerings.underwhirleddrift.UnderwhirledDrift;
import com.threerings.underwhirleddrift.scene.Ground;

public class KartObstacle extends KartSprite
{
    // Fields required by Scenery for properly positioning and sizing scenery objects
    public var sceneryType :int;
    public var startWidth :Number;
    public var startHeight :Number;
    public var transformedOrigin: Point; 
    public var radius :Number;

    public function KartObstacle (startingPosition :Point, kartType :String, ground :Ground) 
    {
        // medium kart is all we have for now
        super(kartType, ground);
        _currentPosition = startingPosition;

        UnderwhirledDrift.registerEventListener(this, Event.ENTER_FRAME, enterFrame);
    }

    /**
     * Getter for the current location of this kart obstacle (opponent kart)
     */
    public function get origin () :Point 
    {
        return _currentPosition;
    }

    public function get sprite () :Sprite
    {
        return this;
    }

    /**
     * Set the position, angle, etc of this kart based on a network update.
     */
    public function setPosition (obj :Object) :void
    {
        _currentPosition = new Point(obj.posX, obj.posY);
        _currentAngle = obj.angle;
        _currentSpeed = obj.speed;
        _movement = obj.movement;
    }

    /**
     * Update the viewed angle of this kart sprite, as seen from the camera, and with regard
     * to the opponent's view angle.
     */
    public function updateAngleFrom (cameraLocation :Point) :void
    {
        var angleFrom :Number = Math.atan2(_currentPosition.y - cameraLocation.y,
            _currentPosition.x - cameraLocation.x) * 180 / Math.PI;
        angleFrom -= _currentAngle * 180 / Math.PI;
        angleFrom += 90;
        while (angleFrom < 0) {
            angleFrom += 360;
        }
        while (angleFrom >= 360) {
            angleFrom -= 360;
        }
        _kart.gotoAndStop(Math.ceil(angleFrom));
    }

    public function enterFrame (event :Event) :void
    {
        calculateNewPosition(timeSinceUpdate());
    }
}
}
