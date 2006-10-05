package {

import flash.display.Scene;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.TimerEvent;
import flash.utils.describeType;
import flash.utils.Timer;

import mx.core.BitmapAsset;
import mx.core.MovieClipAsset;

import com.threerings.ezgame.EZGame;

public class Bunny extends Sprite
{
    public function Bunny (board :Board)
    {
        _board = board;
        bunnyMovie = MovieClipAsset(new bunnyAnim());
        idle();
        addChild(bunnyMovie);
        _moveQueue = new Array();
        _moveStore = new Array();
    }

    public function remoteWalk (delta :int) :void
    {
        if (_attacking) {
            return;
        }
        if (_moveQueue.length == 0) {
            idle();
            return;
        }
        var sum :int = 0;
        var tot :int = 0;
        while (tot < delta && _moveQueue.length > 0) {
            var action :String = String(_moveQueue.shift());
            switch (action) {
              case "w":
                var pop :int = int(_moveQueue.shift());
                tot += Math.abs(pop);
                if (tot > delta) {
                    var remaining :int = tot - delta;
                    var sign :int = pop / Math.abs(pop);
                    sum += sign * (Math.abs(pop) - remaining);
                    _moveQueue.unshift(remaining * sign);
                    _moveQueue.unshift("w");
                } else {
                    sum += pop;
                }
                break;
              case "s":
                if (sum != 0) {
                    walk(sum, false);
                }
                attack();
                return;
            }
        }
        walk(sum, false);
    }

    public function walk (deltaX :int, primary :Boolean = true) :void
    {
        if (_attacking) {
            return;
        }
        if (bunnyMovie.currentFrame != 2) {
            bunnyMovie.gotoAndStop(2);
        }
        var newOrient :int;
        if (deltaX < 0) {
            newOrient = 1;
        } else {
            newOrient = 0;
        }

        if (newOrient != _orient) {
            _orient = newOrient;
            switch (_orient) {
              case 0:
                bunnyMovie.scaleX = 1;
                bunnyMovie.x = 0;
                break;
              case 1:
                bunnyMovie.scaleX = -1;
                bunnyMovie.x = bunnyMovie.width;
                break;
            }
        }
        _board.move(this, deltaX, BWIDTH);
        _moveStore.push("w");
        _moveStore.push(String(deltaX));
    }

    public function sendStore (gameObj :EZGame, index :int) :void
    {
        if (_moveStore.length == 0) {
            return;
        }
        var deltas :String = String(_moveStore.shift());
        while (_moveStore.length > 0) {
            deltas += "," + _moveStore.shift();
        }
        gameObj.sendMessage("bunny" + index, deltas);
    }

    public function remote (queue :String) :void
    {
        _moveQueue = _moveQueue.concat(queue.split(","));
    }

    public function setCoords (x :int, y :int) :void
    {
        _bY = y;
        this.y = _bY + Tile.TILE_SIZE - height;
        _bX = x;
        this.x = _bX - int((width - BWIDTH)/2);
    }

    public function getBX () :int
    {
        return _bX;
    }

    public function getBY () :int
    {
        return _bY;
    }

    public function idle () :void
    {
        if (_attacking) {
            return;
        }
        bunnyMovie.gotoAndStop(1);
    }

    public function attack () :void
    {
        if (_attacking) {
            return;
        }
        _attacking = true;
        _attackTimer = new Timer(200, 1);
        _attackTimer.addEventListener(TimerEvent.TIMER_COMPLETE, attackOver);
        _attackTimer.start();
        bunnyMovie.gotoAndStop(3);
        _moveStore.push("s");
    }

    public function attackOver (event :TimerEvent) :void
    {
        _attacking = false;
        idle();
    }

    protected var bunnyMovie :MovieClipAsset;

    protected var _orient :int;
    protected var _offset :int;
    protected var _bX :int, _bY :int;
    protected var _attacking :Boolean = false;
    protected var _attackTimer :Timer;
    protected var _board :Board;
    protected var _moveQueue :Array;
    protected var _moveStore :Array;
    protected static const BWIDTH :int = 28;
    

    [Embed(source="rsrc/bunny/bunny_knight_blue.swf#bunny_blue_master")]
    protected var bunnyAnim :Class;
}
}
