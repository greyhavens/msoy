package {

import flash.display.Sprite;
import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.external.ExternalInterface;

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
            _myKid = _board.getKid(_myIndex);
            // Listen for keys being pressed and released.
            _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, _myKid.keyDownHandler);
            _gameCtrl.addEventListener(KeyboardEvent.KEY_UP, _myKid.keyUpHandler); 
        }
    }
    
    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }
    
    /** The game control object. */
    protected var _gameCtrl :EZGameControl;
    
    /** Game board. */
    protected var _board :Board;
    
    /** This player's kid. */
    protected var _myKid :Kid;
    
    /** Our player index, or -1 if we're not a player. */
    protected var _myIndex :int;
}
}
