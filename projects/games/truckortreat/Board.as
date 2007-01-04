package {

import flash.display.Bitmap;
import flash.events.TimerEvent;

public class Board extends BaseSprite
{   
    /** Lists of sidewalk coordinates. These are safe spots to start on. */
    public static const SIDEWALK_X :Array = [];
    public static const SIDEWALK_Y :Array = [];
    
    /** The y coordinate of the horizon line. */
    public static const HORIZON :int = 157;
    
    public function Board ()
    {
        super(0, 0, Bitmap(new backgroundAsset()));
        
        // Add kids and cars.
        var kid :Kid = new Kid(180, 200, Kid.IMAGE_GHOST, this);
        addChild(kid);
        _kids[0] = kid;
        var car :Car = new Car(275, 150, 10, Car.DOWN, this);
        _cars[0] = car;
        addChild(car);
        car = new Car(555, _height, 5, Car.UP, this);
        _cars[1] = car;
        addChild(car);
    }
    
    /** Do whatever needs to be done on each clock tick. */
    public function tick (event :TimerEvent) :void
    {
        // Call tick() method on each car and kid.
        var car :Car;
        var kid :Kid;
        for each (car in _cars) {
            car.tick();
        }
        
        for each (kid in _kids) {
            kid.tick();
        }
        
        // TODO: search for collisions and handle those
    }
    
    /** Return list of kids in the game. */
    public function getKids () :Array
    {
        return _kids;
    }
    
    /** A list of characters, one for each player. */
    protected var _kids :Array = [];
    
    /** A list of cars on the board. */
    protected var _cars :Array = [];
    
    /** Background image. */
    [Embed(source="rsrc/background.png")]
    protected var backgroundAsset :Class;
}
}
