package {

import flash.display.Sprite;
import flash.events.KeyboardEvent;
import flash.external.ExternalInterface;
import flash.events.TimerEvent;
import flash.utils.Timer;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

[SWF(width="834", height="539")]
public class TruckOrTreat extends Sprite
    implements Game
{
    public function TruckOrTreat ()
    {
    }
    
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObj = gameObj;
        // Create board and put a kid and car on it.
        _board = new Board();
        addChild(_board);
        _kid = new Kid(180, 200, Kid.IMAGE_GHOST, _board);
        addChild(_kid);
        _car = new Car(275, 110, Car.DOWN, _board);
        addChild(_car);
        
        // Set up a ticker that will control movement and new candy placement.
        var gameTimer :Timer = new Timer(REFRESH_RATE, 0);
        gameTimer.addEventListener(TimerEvent.TIMER, tick);
        gameTimer.start();
        
        // Listen for keys being pressed and released.
        stage.addEventListener(KeyboardEvent.KEY_DOWN, _kid.keyDownHandler);
        stage.addEventListener(KeyboardEvent.KEY_UP, _kid.keyUpHandler);
    }
    
    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }
    
    /** Do whatever needs to be done on each clock tick. */
    protected function tick (event :TimerEvent) :void
    {
        _car.tick();
        _kid.tick();
    }
    
    /** The game object. */
    protected var _gameObj :EZGame;
    
    /** Game board. */
    protected var _board :Board;
    
    /** The player's kid character. */
    protected var _kid :Kid;
    
    /** A car. */
    protected var _car :Car;
    
    /** Number of milliseconds between clock ticks. */
    protected static const REFRESH_RATE :int = 50;
}
}
