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
        log("I feeeeel happy");
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
        log("My index:" + _myIndex);
        _numPlayers = _gameObj.getPlayerCount();
        log("num players:" + _numPlayers);

        _world = new Sprite();
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

        log("Creating board");
        _board = new Board(this, 40, 30);
        for (var xx :int = 0; xx < _board.bwidth; xx++) {
            for (var yy :int = 0; yy < _board.bheight; yy++) {
                var tile :Tile;
                if (xx == 0 || xx == _board.bwidth - 1 ||
                    yy == 0 || yy == _board.bheight - 1) {
                    tile = new Tile(xx, yy);
                } else {
                    tile = new Tile(xx, yy, Tile.TYPE_BLACK, 
                            Tile.LAYER_BACK, Tile.EFFECT_NONE);
                }
                _board.addTile(tile);
            }
        }

        _bunnies = new Array(_numPlayers);
        for (ii = 0; ii < _numPlayers; ii++) {
            var newBunny :Bunny = new Bunny(_board);
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
            log("Message from bunny " + bunIdx);
            if (bunIdx != _myIndex) {
                Bunny(_bunnies[bunIdx]).remote(String(event.value));
            }
        }
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
            _bunny.idle();
            return;
        }
        bunnyTick(null);
    }

    public function bunnyTick (event :TimerEvent) :void
    {
        var delta :int = int((getTimer() - _tick) / 50 * 6);
        _tick = getTimer();
        if (delta == 0) {
            return;
        }
        for (var ii :int = 0; ii < _numPlayers; ii++) {
            if (ii != _myIndex) {
                Bunny(_bunnies[ii]).remoteWalk(delta);
            }
        }
        if (_leftDown != _rightDown) {
            if (_leftDown) {
                delta = -delta;
            }
            _bunny.walk(delta);
            recenter();
        }
        if (getTimer() - _messageTick > 110) {
            _bunny.sendStore(_gameObj, _myIndex);
            _messageTick = getTimer();
        }
    }

    public function recenter () :void
    {
        if (_bunny.getBX() + _world.x > 480) {
            _world.x = Math.max(mask.width - width,
                                480 - _bunny.getBX());
        } else if (_bunny.getBX() + _world.x < 100) {
            _world.x = Math.min(0, 100 - _bunny.getBX());
        }
        if (_bunny.getBY() + _world.y > 380) {
            _world.y = Math.max(mask.height - height,
                                380 - _bunny.getBY());
        } else if (_bunny.getBY() + _world.y < 100) {
            _world.y = Math.min(0, 100 - _bunny.getBY());
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
}
}
