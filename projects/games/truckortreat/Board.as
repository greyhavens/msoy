package {

import flash.display.Bitmap;
import flash.events.TimerEvent;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

public class Board extends BaseSprite
{   
    /** Lists of sidewalk coordinates. These are safe spots to start on. */
    public static const SIDEWALK_X :Array = [];
    public static const SIDEWALK_Y :Array = [];
    
    /** The y coordinate of the horizon line. */
    public static const HORIZON :int = 157;
    
    public function Board (gameCtrl :EZGameControl)
    {
        super(0, 0, Bitmap(new backgroundAsset()));
        
        _gameCtrl = gameCtrl;
        
        // Add kids and cars.
        var kid :Kid = new Kid(180, 200, Kid.IMAGE_GHOST, this);
        addChild(kid);
        _kids[0] = kid;
        kid = new Kid(180, 300, Kid.IMAGE_VAMPIRE, this);
        addChild(kid);
        _kids[1] = kid;
        var car :Car = new Car(275, HORIZON + 10, 10, Car.DOWN, this);
        _cars[0] = car;
        addChild(car);
        car = new Car(555, height - 60, 5, Car.UP, this);
        _cars[1] = car;
        addChild(car);
        
        _gameCtrl.addEventListener(MessageReceivedEvent.TYPE, msgReceived);
        if (gameCtrl.isInPlay()) {
            gameDidStart(null);
        } else {
            _gameCtrl.addEventListener(StateChangedEvent.GAME_STARTED, gameDidStart);
        }
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
