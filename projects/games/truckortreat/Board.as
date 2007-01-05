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
        var car :Car = new Car(275, HORIZON + 10, 10, Car.DOWN, this);
        _cars[0] = car;
        addChild(car);
        car = new Car(555, height - 60, 5, Car.UP, this);
        _cars[1] = car;
        addChild(car);
    }
    
    /** Do whatever needs to be done on each clock tick. */
    public function tick (event :TimerEvent) :void
    {
        // Call tick() on cars to move them.
        var car :Car;
        for each (car in _cars) {
            car.tick();
        }
        
        // Now call tick() on each kid to move them, and look for collisions.
        var kid :Kid;
        for each (kid in _kids) {
            kid.tick();
            
            // TODO: look for collisions with candy too, perhaps before the 
            // cars so if a player gets a health power up at the same time as 
            // a death dealing hit by a car, he or she will survive.
            for each (car in _cars) {
                
            }
        }
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
