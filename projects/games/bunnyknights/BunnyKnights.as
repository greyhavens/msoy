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
        /*
        _board.addTiles(Tile.brick, 0, 0, _board.bwidth, 2);
        _board.addTiles(Tile.brick, 0, 0, 2, _board.bheight);
        _board.addTiles(Tile.brick, _board.bwidth-2, 0, 2, _board.bheight);
        _board.addTiles(Tile.brick, 0, _board.bheight-2, _board.bwidth, 2);

        _board.addTiles(Tile.brick, 2, 52, 20, 2);
        _board.addTiles(Tile.ladder, 14, 52, 2, 6);
        _board.addTiles(Tile.ladder, 20, 44, 2, 8);
        _board.addTiles(Tile.brick, 16, 44, 10, 2);
        var doorIdx :int = _board.addDoor(Tile.door(22, 52));
        _board.addSwitch(Tile.tswitch(14, 44), doorIdx);
        */
        _board.addBlocks(Tile.TYPE_EARTH_GRASS, 0, 28);
        _board.addBlocks(Tile.TYPE_EARTH, 0, 29);
        _board.addBlocks(Tile.TYPE_EARTH_GRASS, 1, 29, 4, 1);
        _board.addBlocks(Tile.TYPE_EARTH_GRASS, 7, 29, 17, 1);
        _board.addBlocks(Tile.TYPE_COLUMN_SHORT, 29, 29, 2, 1);
        _board.addBlocks(Tile.TYPE_COLUMN_SHORT, 39, 29);
        _board.addTile(Tile.statue(12, 25, Tile.TYPE_STATUE_ORANGE));
        _board.addTile(Tile.statue(16, 25, Tile.TYPE_STATUE_ORANGE));
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 22, 28);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 29, 28);
        _board.addBlocks(Tile.TYPE_COLUMN_TALL, 22, 26, 2, 2);
        _board.addBlocks(Tile.TYPE_COLUMN_TALL, 29, 26, 2, 2);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 22, 25, 4, 1);
        _board.addBlocks(Tile.TYPE_BLOCK, 26, 25, 1, 1);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 27, 25, 4, 1);
        _board.addBlocks(Tile.TYPE_CHECKER, 31, 28, 8, 1);
        _board.addTiles(Tile.ladder, 19, 23, 2, 6);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 11, 23);
        _board.addBlocks(Tile.TYPE_CHECKER, 13, 23, 3);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 16, 23);
        _board.addBlocks(Tile.TYPE_CHECKER, 18, 23, 4);
        _board.addBlocks(Tile.TYPE_BLOCK, 22, 23, 1, 2);
        _board.addBlocks(Tile.TYPE_CHECKER, 23, 23, 7, 2);
        _board.addBlocks(Tile.TYPE_BLOCK, 30, 23, 1, 2);
        _board.addBlocks(Tile.TYPE_BLOCK, 31, 23);
        _board.addBlocks(Tile.TYPE_CHECKER, 32, 23, 7, 1);
        _board.addBlocks(Tile.TYPE_BLOCK, 39, 19, 1, 10);
        _board.addTiles(Tile.ladder, 37, 23, 2, 5);
        _board.addBlocks(Tile.TYPE_CHECKER, 9, 21, 2, 2);
        _board.addBlocks(Tile.TYPE_CHECKER, 11, 20, 10, 1);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 2, 20, 2, 1);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 2, 22, 2, 1);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 7, 20, 2, 1);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 7, 22, 2, 1);
        _board.addBlocks(Tile.TYPE_COLUMN_SHORT, 2, 21, 2, 1);
        _board.addBlocks(Tile.TYPE_COLUMN_SHORT, 7, 21, 2, 1);
        _board.addBlocks(Tile.TYPE_CHECKER, 4, 20, 3, 1);
        _board.addBlocks(Tile.TYPE_CHECKER, 4, 22, 3, 1);
        _board.addBlocks(Tile.TYPE_BLOCK, 2, 17, 1, 3);
        _board.addTiles(Tile.ladder, 11, 20, 2, 3);
        _board.addBlocks(Tile.TYPE_BLOCK_REC, 21, 20, 4, 1);
        _board.addBlocks(Tile.TYPE_COLUMN_GIANT, 21, 16, 2, 4);
        _board.addBlocks(Tile.TYPE_CHECKER, 2, 15, 38, 1);
        _board.addBlocks(Tile.TYPE_BLOCK, 39, 10, 1, 5);
        _board.addTiles(Tile.ladder, 17, 15, 2, 5);
        _board.addBlocks(Tile.TYPE_COLUMN_GIANT, 2, 11, 2, 4);
        _board.addBlocks(Tile.TYPE_CHECKER, 1, 7, 30, 1);
        _board.addBlocks(Tile.TYPE_CHECKER, 33, 7, 7, 1);
        _board.addBlocks(Tile.TYPE_COLUMN_GIANT, 38, 3, 2, 4);
        _board.addTiles(Tile.ladder, 36, 7, 2, 8);

        
        var doorIdx :int = _board.addDoor(Tile.door(9, 23));
        _board.addSwitch(Tile.tswitch(5, 29), doorIdx);
        _board.addSwitch(Tile.tswitch(9, 20), doorIdx);

        doorIdx = _board.addDoor(Tile.door(27, 1));
        _board.addSwitch(Tile.tswitch(31, 7), doorIdx);

        _bunnies = new Array(_numPlayers);
        for (var ii : int = 0; ii < _numPlayers; ii++) {
            var newBunny :Bunny = new Bunny(_board, ii);
            _bunnies[ii] = newBunny;
            _board.addBunny(newBunny, 1, 28);
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

    public function getBunnies () :Array
    {
        return _bunnies;
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
        } else if (name == "off") {
            var coords :Array = String(event.value).split(",");
            _board.tswitch(false, coords[0], coords[1], coords[2]);
        } else if (name == "on") {
            coords = String(event.value).split(",");
            _board.tswitch(true, coords[0], coords[1], coords[2]);
        }
    }

    public function tswitch (on :Boolean, x :int, y :int, idx :int) :void
    {
        var msg :String = "off";
        if (on) msg = "on";
        _gameObj.sendMessage(msg, String(x) + "," + y + "," + idx);    
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
        var now :int = getTimer();
        var delta :int = int((now - _tick) / 50 * 6);
        _tick = now - (now % 50);
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
            _messageTick = now;
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
