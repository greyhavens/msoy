package 
{

import flash.display.Sprite;

import flash.geom.Point;

public class KartObstacle extends KartSprite
{
    // Fields required by Scenery for properly positioning and sizing scenery objects
    public var startWidth :Number;
    public var startHeight :Number;
    public var transformedOrigin: Point; 

    public function KartObstacle (startingPosition :Point) 
    {
        // medium kart is all we have for now
        super(KART_MEDIUM);
        _currentPosition = startingPosition;
    }

    /**
     * Getter for the current location of this kart obstacle (opponent kart)
     */
    public function get origin () :Point 
    {
        // TODO update this with the real location of the opponent's kart
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
    }

    /**
     * Update the viewed angle of this kart sprite, as seen from the camera, and with regard
     * to the opponent's view angle.
     */
    public function updateAngleFrom (cameraLocation :Point) :void
    {
        var angleFrom :Number = Math.atan2(_currentPosition.y - cameraLocation.y,
            _currentPosition.x - cameraLocation.x) * 180 / Math.PI;
        angleFrom += _currentAngle * 180 / Math.PI;
        angleFrom += 90;
        while (angleFrom < 0) {
            angleFrom += 360;
        }
        while (angleFrom >= 360) {
            angleFrom -= 360;
        }
        _kart.gotoAndStop(Math.ceil(angleFrom));
    }
    
    protected var _currentPosition :Point;
    protected var _currentAngle :Number;
}
}
