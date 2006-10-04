package {

import flash.display.DisplayObject;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.TimerEvent;
import flash.external.ExternalInterface;
import flash.ui.Keyboard;
import flash.utils.Timer;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

[SWF(width="640", height="480")]
public class BunnyKnights extends Sprite
    implements Game, PropertyChangedListener, StateChangedListener
{
    public function BunnyKnights ()
    {
    }

    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        log("Got game object");
        _gameObject = gameObj;
        graphics.clear();
        graphics.beginFill(0x000000);
        graphics.drawRect(0, 0, 640, 480);
        graphics.endFill();

        _layers = new Array(Tile.LAYER_HUD + 1);
        for (var ii :int = 0; ii < Tile.LAYER_HUD; ii++) {
            _layers[ii] = new Sprite();
            addChild(Sprite(_layers[ii]));
        }

        log("Creating board");
        _board = new Board(this);
        for (var xx :int = 0; xx < Board.BOARD_WIDTH; xx++) {
            for (var yy :int = 0; yy < Board.BOARD_HEIGHT; yy++) {
                if (xx == 0 || xx == Board.BOARD_WIDTH - 1 ||
                    yy == 0 || yy == Board.BOARD_HEIGHT - 1) {
                    var tile :Tile = new Tile(xx, yy);
                    _board.addTile(tile);
                }
            }
        }

        log("Creating bunny");
        _bunny = new Bunny();
        addChildToLayer(_bunny, Tile.LAYER_ACTION_FRONT);
        _bunny.y = 300;
        stage.addEventListener(KeyboardEvent.KEY_DOWN, keyDownHandler);
        stage.addEventListener(KeyboardEvent.KEY_UP, keyUpHandler);
    }

    public function addChildToLayer (child :DisplayObject, layer :int)
        :DisplayObject
    {
        return Sprite(_layers[layer]).addChild(child);
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
    }

    protected function keyUpHandler (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
          case Keyboard.LEFT:
            _leftDown = false;
            moveBunny();
            break;
          case Keyboard.RIGHT:
            _rightDown = false;
            moveBunny();
            break;
        }
    }

    protected function keyDownHandler (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
          case Keyboard.LEFT:
            _leftDown = true;
            moveBunny();
            break;
          case Keyboard.RIGHT:
            _rightDown = true;
            moveBunny();
            break;
          case Keyboard.SPACE:
            _bunny.attack();
            break;
        }
    }

    protected function moveBunny () :void
    {
        if (_leftDown == _rightDown) {
            if (_moveTimer != null) {
                _moveTimer.stop();
                _moveTimer = null;
                _bunny.idle();
            }
            return;
        } else if (_moveTimer != null) {
            return;
        }
        _moveTimer = new Timer(50);
        _moveTimer.addEventListener(TimerEvent.TIMER, bunnyTick);
        _moveTimer.start();
        bunnyTick(null);
    }

    public function bunnyTick (event :TimerEvent) :void
    {
        if (_leftDown) {
            _bunny.walk(-3);
        } else {
            _bunny.walk(3);
        }
    }

    /** Out game object. */
    protected var _gameObject :EZGame;

    protected var _bunny :Bunny;

    protected var _moveTimer :Timer;

    protected var _board :Board;
    protected var _layers :Array;
    
    protected var _leftDown :Boolean = false, _rightDown :Boolean = false;
}
}
