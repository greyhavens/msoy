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

    /**
     * the sprite field is also read-only
     */
    public function get sprite () :Sprite
    {
        return _kart;   
    }

    /**
     * Set the position, angle, etc of this kart based on a network update.
     */
    public function setPosition (obj :Object) :void
    {
        _currentPosition = new Point(obj.posX, obj.posY);
    }
    
    protected var _currentPosition :Point;
}
}
