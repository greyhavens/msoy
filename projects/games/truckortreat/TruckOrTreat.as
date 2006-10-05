package {

import flash.display.Sprite;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;
import flash.external.ExternalInterface;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

[SWF(width="512", height="512")]
public class TruckOrTreat extends Sprite
    implements Game
{
    public function TruckOrTreat ()
    {
    }
    
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObj = gameObj;
        // Create board and put a kid on it.
        _board = new Board(gameObj);
        addChild(_board);
        _kid = new Kid(1, 5);
        addChild(_kid);
        
        // Listen for keys being hit
        stage.addEventListener(KeyboardEvent.KEY_DOWN, keyHandler);
    }
    
    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }
    
    protected function keyHandler(event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.UP:
            _kid.move(0, -1);
            break;
        case Keyboard.DOWN:
            _kid.move(0, 1);
            break;
        case Keyboard.LEFT:
            _kid.move(-1, 0);
            break;
        case Keyboard.RIGHT:
            _kid.move(1, 0);
            break;
        default:
            return;
        }
    }
    
    /** The game object. */
    protected var _gameObj :EZGame;
    
    /** Game board. */
    protected var _board :Board;
    
    /** The player's kid character. */
    protected var _kid :Kid
}
}
