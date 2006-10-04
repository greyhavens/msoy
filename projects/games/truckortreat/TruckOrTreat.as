package {

import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

[SWF(width="600", height="400")]
public class TruckOrTreat extends Sprite
    implements Game
{
    public function TruckOrTreat ()
    {
    }
    
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObj = gameObj;
        _board = new Board(gameObj);
        _gameObj.localChat("Run fast, get candy, and don't get run over!")
    }
    
    /** The game object. */
    protected var _gameObj :EZGame;
    
    /** Game board. */
    protected var _board :Board;
    
    /** The player's character. */
    protected var _kid :Kid
    
}
}