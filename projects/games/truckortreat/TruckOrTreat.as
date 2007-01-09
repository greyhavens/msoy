package {

import flash.display.Sprite;
import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

[SWF(width="834", height="539")]
public class TruckOrTreat extends Sprite
{
    public function TruckOrTreat ()
    {
        _gameCtrl = new EZGameControl(this);        
        // Create board. This will create the cars and kids, too.
        addChild(_board = new Board(_gameCtrl));
        _myIndex = _gameCtrl.getMyIndex();
        
        if (_myIndex != -1) {
            // Listen for keys being pressed.
            _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, keyDownHandler);
        }
    }
    
    /** 
     * Send message indicating what direction(s) to move when a given key is 
     * being pressed or held down.
     */
    protected function keyDownHandler (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.UP:
            _gameCtrl.sendMessage("kid" + _myIndex, Direction.UP);
            break;
        case Keyboard.DOWN:
            _gameCtrl.sendMessage("kid" + _myIndex, Direction.DOWN);
            break;
        case Keyboard.LEFT:
            _gameCtrl.sendMessage("kid" + _myIndex, Direction.LEFT);
            break;
        case Keyboard.RIGHT:
            _gameCtrl.sendMessage("kid" + _myIndex, Direction.RIGHT);
            break;
        default:
            return;
        }
    }
    
    /** The game control object. */
    protected var _gameCtrl :EZGameControl;
    
    /** Game board. */
    protected var _board :Board;
    
    /** Our player index, or -1 if we're not a player. */
    protected var _myIndex :int;
}
}
