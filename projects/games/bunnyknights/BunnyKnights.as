package {

import flash.display.DisplayObject;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.TimerEvent;
import flash.external.ExternalInterface;
import flash.geom.Point;
import flash.ui.Keyboard;
import flash.utils.Timer;
import flash.utils.getTimer;

import com.threerings.util.Random;

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
    implements Game, PropertyChangedListener, StateChangedListener, 
            MessageReceivedListener
{
    public function BunnyKnights ()
    {
        var square :Sprite = new Sprite();
        square.graphics.beginFill(0x000000);
        square.graphics.drawRect(0, 0, 640, 480);
        square.graphics.endFill();
        addChild(square);

        mask = square;
    }

    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObj = gameObj;
        _myIndex = _gameObj.getMyIndex();
        _numPlayers = _gameObj.getPlayerCount();

        _world = new Sprite();
        _world.scaleX = 2;
        _world.scaleY = 2;
        addChild(_world);
        _layers = new Array(Tile.LAYER_HUD + 1);
        for (var ii :int = 0; ii <= Tile.LAYER_HUD; ii++) {
            _layers[ii] = new Sprite();
            if (ii < Tile.LAYER_HUD) {
                _world.addChild(Sprite(_layers[ii]));
            } else {
                addChild(Sprite(_layers[ii]));
            }
        }

        if (_myIndex == 0) {
            _gameObj.sendMessage("rseed", getTimer());
        }
        startGame();
    }

    public function startGame () :void
    {
        log("Creating board");
        _board = new Board(this, 40, 30);
        var random :Random = new Random(7);
        _board.addTiles(Tile.brick, 0, 0, _board.bwidth);
        _board.addTiles(Tile.brick, 0, 0, 1, _board.bheight);
        _board.addTiles(Tile.brick, _board.bwidth-1, 0, 1, _board.bheight);
        _board.addTiles(Tile.brick, 0, _board.bheight-1, _board.bwidth);

        _board.addTiles(Tile.brick, 1, 25, 10, 1);
        _board.addTiles(Tile.ladder, 7, 25, 1, 4);
        _board.addTiles(Tile.ladder, 10, 22, 1, 3);
        _board.addTiles(Tile.brick, 8, 22, 5, 1);

        _bunnies = new Array(_numPlayers);
        for (var ii : int = 0; ii < _numPlayers; ii++) {
            var newBunny :Bunny = new Bunny(_board, ii);
            _bunnies[ii] = newBunny;
            _board.addBunny(newBunny, 1, _board.bheight - 2);
            if (ii == _myIndex) {
                _bunny = newBunny;
            }
        }
        recenter();
        stage.addEventListener(KeyboardEvent.KEY_DOWN, keyDownHandler);
        stage.addEventListener(KeyboardEvent.KEY_UP, keyUpHandler);

        _moveTimer = new Timer(10);
        _moveTimer.addEventListener(TimerEvent.TIMER, bunnyTick);
        _moveTimer.start();
        _tick = getTimer();
        _messageTick = getTimer();
    }

    public function getLayer (layer :int) :Sprite
    {
        return Sprite(_layers[layer]);
    }

    public function addChildToLayer (child :DisplayObject, layer :int)
        :DisplayObject
    {
        return getLayer(layer).addChild(child);
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
        var name :String = event.name;
        if (name.indexOf("bunny") == 0) {
            var bunIdx :int = int(name.substring(5));
            if (bunIdx != _myIndex) {
                Bunny(_bunnies[bunIdx]).remote(String(event.value));
            }
        }
    }

    protected function keyUpHandler (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
          case Keyboard.LEFT:
            if (_leftDown) _keysDown--;
            _leftDown = false;
            break;
          case Keyboard.RIGHT:
            if (_rightDown) _keysDown--;
            _rightDown = false;
            break;
          case Keyboard.UP:
            if (_upDown) _keysDown--;
            _upDown = false;
            break;
          case Keyboard.DOWN:
            if (_downDown) _keysDown--;
            _downDown = false;
            break;
        }
    }

    protected function keyDownHandler (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
          case Keyboard.LEFT:
            if (!_leftDown) _keysDown++;
            _leftDown = true;
            break;
          case Keyboard.RIGHT:
            if (!_rightDown) _keysDown++;
            _rightDown = true;
            break;
          case Keyboard.UP:
            if (!_upDown) _keysDown++;
            _upDown = true;
            break;
          case Keyboard.DOWN:
            if (!_downDown) _keysDown++;
            _downDown = true;
            break;
          case Keyboard.SPACE:
            _bunny.attack();
            break;
        }
    }

    public function bunnyTick (event :TimerEvent) :void
    {
        var delta :int = int((getTimer() - _tick) / 50 * 6);
        _tick = getTimer();
        if (_keysDown != 1) {
            _bunny.idle();
        }
        if (delta == 0) {
            return;
        }
        for (var ii :int = 0; ii < _numPlayers; ii++) {
            if (ii != _myIndex) {
                Bunny(_bunnies[ii]).remoteWalk(delta);
            }
        }
        if (_keysDown == 1) { 
            if (_leftDown || _rightDown) {
                if (_leftDown) {
                    delta = -delta;
                }
                _bunny.walk(delta);
            } else if (_upDown || _downDown) {
                if (_upDown) {
                    delta = -delta;
                }
                _bunny.climb(delta);
            }
            recenter();
        }
        if (getTimer() - _messageTick > 110) {
            _bunny.sendStore(_gameObj, _myIndex);
            _messageTick = getTimer();
        }
    }

    public function recenter () :void
    {
        var bx :int = _bunny.getBX()*2;
        var by :int = _bunny.getBY()*2;
        if (bx + _world.x > 400) {
            _world.x = Math.max(mask.width - _world.width, 400 - bx);
        } else if (bx + _world.x < 180) {
            _world.x = Math.min(0, 180 - bx);
        }
        if (by + _world.y > 300) {
            _world.y = Math.max(mask.height - _world.height, 300 - by);
        } else if (by + _world.y < 150) {
            _world.y = Math.min(0, 150 - by);
        }
    }

    /** Out game object. */
    protected var _gameObj :EZGame;

    protected var _bunny :Bunny;
    protected var _bunnies :Array;

    protected var _moveTimer :Timer;
    protected var _tick :uint;
    protected var _messageTick :uint;

    protected var _board :Board;
    protected var _layers :Array;
    protected var _world :Sprite;

    protected var _myIndex :int;
    protected var _numPlayers :int;
    
    protected var _leftDown :Boolean = false, _rightDown :Boolean = false;
    protected var _upDown :Boolean = false, _downDown :Boolean = false;
    protected var _keysDown :int;
}
}
