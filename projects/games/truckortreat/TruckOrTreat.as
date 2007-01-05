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
        // Create board. This will create the cars and kids, too.
        addChild(_board = new Board(_gameObj));
        _kids = _board.getKids();
        
        // Set up a ticker that will control movement and new candy placement.
        var gameTimer :Timer = new Timer(REFRESH_RATE, 0);
        gameTimer.addEventListener(TimerEvent.TIMER, _board.tick);
        gameTimer.start();
        
        // Listen for keys being pressed and released.
        stage.addEventListener(KeyboardEvent.KEY_DOWN, _kids[0].keyDownHandler);
        stage.addEventListener(KeyboardEvent.KEY_UP, _kids[0].keyUpHandler);
    }
    
    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }
    
    /** The game object. */
    protected var _gameObj :EZGame;
    
    /** Game board. */
    protected var _board :Board;
    
    /** A list of kids, one for each player. */
    protected var _kids :Array = [];
    
    /** Number of milliseconds between clock ticks. */
    protected static const REFRESH_RATE :int = 50;
}
}
