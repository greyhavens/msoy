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
        _startingPosition = startingPosition;
    }

    /**
     * Getter for the current location of this kart obstacle (opponent kart)
     */
    public function get origin () :Point 
    {
        // TODO update this with the real location of the opponent's kart
        return _startingPosition;
    }

    /**
     * the sprite field is also read-only
     */
    public function get sprite () :Sprite
    {
        return _kart;   
    }
    
    protected var _startingPosition :Point;
}
}
