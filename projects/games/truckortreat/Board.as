package {

import flash.display.Bitmap;
import flash.events.TimerEvent;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

public class Board extends BaseSprite
{
    /** The y coordinate of the horizon line. */
    public static const HORIZON :int = 157;
    
    /** X coordinate of left side of each sidewalk. */
    public static const LEFT_SIDEWALK :int = 180;
    public static const RIGHT_SIDEWALK :int = 634;
    
    public function Board (gameCtrl :EZGameControl)
    {
        super(0, 0, Bitmap(new backgroundAsset()));
        
        _gameCtrl = gameCtrl;
        
        // Add kids and cars.
        var names :Array = gameCtrl.getPlayerNames();
        var kid :Kid;
        var i :int;
        for (i = 0; i < names.length; i++) {
            // TODO: the height we are using to adjust the starting y 
            // coordinate is just made up here for now. Need to find a good way 
            // of determining the actual height of the bitmap before the kid 
            // sprite is instantiated.
            // Also, we want to let the player choose the image to use rather 
            // than just grabbing one corresponding to his/her index.
            kid = new Kid(getSidewalkX(), getSidewalkY() - 45, i, names[i], this);
            _kids[i] = kid;
            addChild(kid);
        }
        // TODO non-hard coded car creation.
        var car :Car = new Car(275, HORIZON + 10, 10, Car.DOWN, this);
        _cars[0] = car;
        addChild(car);
        car = new Car(555, height - 60, 5, Car.UP, this);
        _cars[1] = car;
        addChild(car);
        
        gameCtrl.addEventListener(MessageReceivedEvent.TYPE, msgReceived);
        if (gameCtrl.isInPlay()) {
            gameDidStart(null);
        } else {
            gameCtrl.addEventListener(StateChangedEvent.GAME_STARTED, gameDidStart);
        }
    }
    
    /** Returns the X coordinate of the left side of a random sidewalk. */
    public function getSidewalkX () :int
    {
        if (Math.random() < 0.5) {
            return LEFT_SIDEWALK;
        } else {
            return RIGHT_SIDEWALK;
        }
    }
    
    /** Returns a random Y coordinate of a point on a sidewalk. */
    public function getSidewalkY () :int
    {
        return int(Math.random() * (height - HORIZON)) + HORIZON;
    }
    
    /** Do whatever needs to be done on each clock tick. */
    protected function doTick () :void
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
            
            if (kid.isAlive()) {
                // TODO: look for collisions with candy too, perhaps before the 
                // cars so if a player gets a health power up at the same time as 
                // a death dealing hit by a car, he or she will survive.
                for each (car in _cars) {
                    // We only need to look for collisions if the kid's feet 
                    // intersect with the bottom half of the car. 
                    if (car.y + car.height > kid.y + kid.height && 
                        kid.y + kid.height > car.y + car.height/2) {
                        if (kid.hitTestObject(car)) {
                            kid.wasKilled();
                            if (kid.livesLeft() <= 0) {
                                // TODO: endGame() takes one or more winning player 
                                // indices. Since we only have one player currently, 
                                // make that one the winner despite having just died.
                                //_gameCtrl.endGame.(0);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /** Called when game is ready to start. */
    protected function gameDidStart (event :StateChangedEvent) :void
    {
        // Player 0 starts the ticker.
        if (_gameCtrl.getMyIndex() == 0) {
            _gameCtrl.startTicker("tick", 100);
        }
    }
    
    /** Handles MessageReceivedEvents. */
    protected function msgReceived (event :MessageReceivedEvent) :void
    {
        if (event.name == "tick") {
            doTick();
        } else if (event.name.indexOf("kid") == 0) {
            var kidIndex :int = int(event.name.substring(3));
            var kid :Kid = Kid(_kids[kidIndex]);
            kid.setMove(event.value as int);
        }
    }
    
    /** The game controller object. */
    protected var _gameCtrl :EZGameControl;
    
    /** A list of characters, one for each player. */
    protected var _kids :Array = [];
    
    /** A list of cars on the board. */
    protected var _cars :Array = [];
    
    /** Background image. */
    [Embed(source="rsrc/background.png")]
    protected var backgroundAsset :Class;
}
}
