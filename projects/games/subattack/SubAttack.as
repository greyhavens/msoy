package {

import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.ui.Keyboard;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;

[SWF(width="400", height="400")]
public class SubAttack extends Sprite
    implements Game, PropertyChangedListener, MessageReceivedListener
{
    /** The size of a tile. */
    public static const TILE_SIZE :int = 24;

    public static const VISION_TILES :int = 8;

    public static const VIEW_TILES :int = (VISION_TILES * 2) + 1;

    public function SubAttack ()
    {
        addChild(_seaDisplay = new SeaDisplay());

        // this might make it so that keyboard focus is grabbed out of the gate
        tabEnabled = true;
        tabIndex = 1;

        var maskSize :int = VIEW_TILES * TILE_SIZE;
        var masker :Shape = new Shape();
        masker.graphics.beginFill(0xFFFFFF);
        masker.graphics.drawRect(0, 0, maskSize, maskSize);
        masker.graphics.endFill();
        this.mask = masker;
        addChild(masker);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObj = gameObj;
        _board = new Board(gameObj, _seaDisplay);
        _myIndex = _gameObj.getMyIndex();

        if (_myIndex != -1) {
            stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        }
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        var index :int = event.index;

        // nada
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
        // nada
    }

    /**
     * Handles KEY_DOWN.
     */
    protected function keyEvent (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.DOWN:
        case Keyboard.UP:
        case Keyboard.RIGHT:
        case Keyboard.LEFT:
        case Keyboard.SPACE:
            _gameObj.sendMessage("sub" + _myIndex, event.keyCode);
            break;

        default:
            return;
        }
    }

    /** The game object. */
    protected var _gameObj :EZGame;

    /** Represents our board. */
    protected var _board :Board;

    protected var _seaDisplay :SeaDisplay;

    protected var _myIndex :int;
}
}
